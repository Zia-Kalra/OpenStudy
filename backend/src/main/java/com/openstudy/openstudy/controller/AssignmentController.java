package com.openstudy.openstudy.controller;

import com.openstudy.openstudy.service.GeminiAiService;
import com.openstudy.openstudy.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

@RestController
@RequestMapping("/api/assignments")
@Profile("!mock")
public class AssignmentController {
    private final JdbcTemplate db;
    private final GeminiAiService ai;
    private final Path uploads = Path.of(System.getProperty("openstudy.upload-dir", "uploads/assignments")).toAbsolutePath().normalize();

    public AssignmentController(JdbcTemplate db, GeminiAiService ai) { this.db = db; this.ai = ai; }

    private record Identity(long id, String role) {}
    private Identity identity(HttpSession session, String role) {
        Object u = session.getAttribute("user");
        Object id = session.getAttribute("userId");
        Object r = session.getAttribute("role");
        if (u instanceof User user) { id = user.getUserId(); r = user.getRole(); }
        if (id == null || r == null || !role.equalsIgnoreCase(r.toString()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in with the required account to continue.");
        try {
            long userId=Long.parseLong(id.toString());
            List<Map<String,Object>> account=db.queryForList("SELECT role,account_status FROM users WHERE user_id=?",userId);
            if(account.isEmpty() || !role.equalsIgnoreCase(account.get(0).get("role").toString()) || !"ACTIVE".equals(account.get(0).get("account_status").toString()))
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Your account is not active for this action.");
            return new Identity(userId,role);
        }
        catch (NumberFormatException e) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED); }
    }
    private void requireTeacherOwns(long assignment, long teacher) {
        if (db.queryForList("SELECT assignment_id FROM assignments WHERE assignment_id=? AND created_by=?", Long.class, assignment, teacher).isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    private Map<String,Object> one(String sql, Object... args) {
        try { return db.queryForMap(sql,args); } catch (EmptyResultDataAccessException e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND); }
    }

    @GetMapping("/subjects")
    public List<Map<String,Object>> subjects(HttpSession session) {
        Identity me = identity(session, "TEACHER");
        return db.queryForList("SELECT s.subject_id,s.subject_name FROM subjects s JOIN subject_teachers st ON st.subject_id=s.subject_id WHERE st.teacher_id=? ORDER BY s.subject_name", me.id());
    }

    @PostMapping("/subjects") @Transactional
    public Map<String,Object> addSubject(@RequestBody Map<String,Object> in, HttpSession session) {
        Identity me = identity(session, "TEACHER");
        String name = text(in, "subjectName", 100, true);
        List<Map<String,Object>> existing = db.queryForList("SELECT subject_id,subject_name FROM subjects WHERE LOWER(TRIM(subject_name))=LOWER(TRIM(?))", name);
        if (!existing.isEmpty()) {
            long existingId = ((Number) existing.get(0).get("subject_id")).longValue();
            if (!db.queryForList("SELECT subject_id FROM subject_teachers WHERE subject_id=? AND teacher_id=?", Integer.class, existingId, me.id()).isEmpty())
                throw new ResponseStatusException(HttpStatus.CONFLICT, "That subject is already in your available options.");
            db.update("INSERT INTO subject_teachers(subject_id,teacher_id) VALUES(?,?)", existingId, me.id());
            return Map.of("subject_id", existingId, "subject_name", existing.get(0).get("subject_name"));
        }
        long subjectId;
        try {
            subjectId = insertAndGetId("INSERT INTO subjects(student_id,subject_name,description) VALUES(NULL,?,?)", name, "");
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Teacher-created subjects require the subjects.student_id nullable change in V4. Apply V4 before adding subjects.", e);
        }
        db.update("INSERT INTO subject_teachers(subject_id,teacher_id) VALUES(?,?)", subjectId, me.id());
        return Map.of("subject_id", subjectId, "subject_name", name);
    }

    @PostMapping
    public Map<String,Object> create(@RequestBody Map<String,Object> in, HttpSession session) {
        Identity me = identity(session,"TEACHER");
        String title = text(in,"title",200,true), instructions = text(in,"instructions",10000,true);
        int subject = integer(in,"subjectId",1, Integer.MAX_VALUE);
        int hours = optionalInteger(in,"hours",0, 24*30), minutes = optionalInteger(in,"minutes",0,59);
        if (hours*60+minutes < 1 || hours*60+minutes > 24*30) throw bad("Attempt duration must be between 1 minute and 30 days.");
        BigDecimal max = decimal(in,"maxScore");
        if (max.signum() <= 0) throw bad("Maximum marks must be greater than zero.");
        LocalDateTime due;
        try { due=LocalDateTime.parse(text(in,"dueAt",30,true)); }
        catch (Exception e) { throw bad("Enter a valid submission deadline."); }
        if(!due.isAfter(LocalDateTime.now())) throw bad("Submission deadline must be in the future.");
        if (!db.queryForList("SELECT subject_id FROM subject_teachers WHERE subject_id=? AND teacher_id=?", Integer.class, subject, me.id()).contains(subject)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        long id = insertAndGetId("INSERT INTO assignments(subject_id,created_by,title,instructions,due_at,max_score,status,attempt_duration_minutes) VALUES(?,?,?,?,?,?,'DRAFT',?)",
                subject, me.id(), title, instructions, due, max, hours*60+minutes);
        return Map.of("assignmentId",id,"status","DRAFT");
    }

    @PostMapping("/{id}/questions") @Transactional
    public Map<String,Object> questions(@PathVariable long id,@RequestBody Map<String,Object> in,HttpSession session) {
        Identity me=identity(session,"TEACHER"); requireTeacherOwns(id,me.id());
        Map<String,Object> a=one("SELECT max_score,status FROM assignments WHERE assignment_id=?",id);
        if (!"DRAFT".equals(a.get("status").toString())) throw bad("Questions can only be edited while the assignment is a draft.");
        String q=text(in,"questionText",5000,true), type=text(in,"questionType",20,true), answer=text(in,"modelAnswer",10000,true), rubric=text(in,"rubric",10000,false);
        if (!List.of("SHORT_TEXT","LONG_TEXT").contains(type)) throw bad("Choose SHORT_TEXT or LONG_TEXT.");
        BigDecimal points=decimal(in,"points"); if(points.signum()<=0) throw bad("Question marks must be positive.");
        Integer pos=db.queryForObject("SELECT COALESCE(MAX(position),0)+1 FROM assignment_questions WHERE assignment_id=?",Integer.class,id);
        long questionId = insertAndGetId("INSERT INTO assignment_questions(assignment_id,question_text,question_type,model_answer,rubric,points,position) VALUES(?,?,?,?,?,?,?)",id,q,type,answer,rubric,points,pos);
        return Map.of("questionId",questionId,"position",pos);
    }

    @PutMapping("/{id}/questions/{questionId}") @Transactional
    public Map<String,Object> editQuestion(@PathVariable long id,@PathVariable long questionId,@RequestBody Map<String,Object> in,HttpSession session) {
        Identity me=identity(session,"TEACHER");requireTeacherOwns(id,me.id());
        if(db.queryForList("SELECT assignment_id FROM assignments WHERE assignment_id=? AND status='DRAFT'",Long.class,id).isEmpty())throw bad("Only draft questions can be changed.");
        String q=text(in,"questionText",5000,true),type=text(in,"questionType",20,true),answer=text(in,"modelAnswer",10000,true),rubric=text(in,"rubric",10000,false);BigDecimal points=decimal(in,"points");
        if(!List.of("SHORT_TEXT","LONG_TEXT").contains(type)||points.signum()<=0)throw bad("Question type and marks are invalid.");
        if(db.update("UPDATE assignment_questions SET question_text=?,question_type=?,model_answer=?,rubric=?,points=? WHERE assignment_id=? AND question_id=?",q,type,answer,rubric,points,id,questionId)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return Map.of("saved",true);
    }

    @DeleteMapping("/{id}/questions/{questionId}") @Transactional
    public Map<String,Object> deleteQuestion(@PathVariable long id,@PathVariable long questionId,HttpSession session) {
        Identity me=identity(session,"TEACHER");requireTeacherOwns(id,me.id());
        if(db.queryForList("SELECT assignment_id FROM assignments WHERE assignment_id=? AND status='DRAFT'",Long.class,id).isEmpty())throw bad("Only draft questions can be removed.");
        if(db.update("DELETE FROM assignment_questions WHERE assignment_id=? AND question_id=?",id,questionId)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return Map.of("deleted",true);
    }

    @PostMapping("/{id}/generate-questions")
    public Map<String,Object> generate(@PathVariable long id,@RequestBody Map<String,Object> in,HttpSession session) {
        Identity me=identity(session,"TEACHER"); requireTeacherOwns(id,me.id());
        int count=integer(in,"count",1,20); BigDecimal points=decimal(in,"points");
        if(points.signum()<=0) throw bad("Points must be positive.");
        String type=text(in,"questionType",20,true); if(!List.of("SHORT_TEXT","LONG_TEXT").contains(type)) throw bad("Invalid question type.");
        return Map.of("questions",ai.generateAssignmentQuestions(text(in,"topic",300,true),count,type,text(in,"difficulty",20,true),points));
    }

    @PostMapping("/{id}/publish") @Transactional
    public Map<String,Object> publish(@PathVariable long id,HttpSession session) {
        Identity me=identity(session,"TEACHER"); requireTeacherOwns(id,me.id());
        Map<String,Object> a=one("SELECT max_score,status,due_at,attempt_duration_minutes FROM assignments WHERE assignment_id=?",id);
        List<Map<String,Object>> qs=db.queryForList("SELECT points,model_answer,question_text,question_type FROM assignment_questions WHERE assignment_id=? ORDER BY position",id);
        if(!"DRAFT".equals(a.get("status").toString()) || qs.isEmpty()) throw bad("A draft must contain at least one valid question before publishing.");
        BigDecimal total=BigDecimal.ZERO, max=(BigDecimal)a.get("max_score");
        for(var q:qs){ if(q.get("model_answer")==null || q.get("model_answer").toString().isBlank()) throw bad("Every question needs a model answer."); total=total.add((BigDecimal)q.get("points")); }
        if(total.compareTo(max)!=0) throw bad("Question marks must add up to the assignment maximum.");
        if(a.get("due_at")==null) throw bad("Set a submission deadline before publishing.");
        db.update("UPDATE assignments SET status='PUBLISHED' WHERE assignment_id=? AND created_by=?",id,me.id());
        return Map.of("assignmentId",id,"status","PUBLISHED");
    }

    @GetMapping("/teacher")
    public List<Map<String,Object>> teacherAssignments(HttpSession session) {
        Identity me=identity(session,"TEACHER");
        return db.queryForList("SELECT a.assignment_id,a.title,a.status,a.created_at,a.due_at,a.max_score,s.subject_name,(SELECT COUNT(*) FROM assignment_questions q WHERE q.assignment_id=a.assignment_id) question_count FROM assignments a JOIN subjects s ON s.subject_id=a.subject_id WHERE a.created_by=? ORDER BY a.created_at DESC",me.id());
    }

    @GetMapping("/teacher/{id}")
    public Map<String,Object> teacherAssignment(@PathVariable long id,HttpSession session) {
        Identity me=identity(session,"TEACHER");requireTeacherOwns(id,me.id());
        Map<String,Object> a=one("SELECT a.assignment_id,a.title,a.instructions,a.status,a.max_score,a.due_at,a.created_at,s.subject_name FROM assignments a JOIN subjects s ON s.subject_id=a.subject_id WHERE a.assignment_id=? AND a.created_by=?",id,me.id());
        a.put("questions",db.queryForList("SELECT question_id,question_text,question_type,model_answer,rubric,points,position FROM assignment_questions WHERE assignment_id=? ORDER BY position",id));return a;
    }

    @GetMapping
    public List<Map<String,Object>> published(HttpSession session) {
        Identity me=identity(session,"STUDENT");
        return db.queryForList("SELECT a.assignment_id,a.title,a.instructions,a.due_at,a.max_score,a.attempt_duration_minutes,a.status,s.subject_name,(SELECT COUNT(*) FROM assignment_questions q WHERE q.assignment_id=a.assignment_id) question_count FROM assignments a JOIN subjects s ON s.subject_id=a.subject_id JOIN subject_enrollments e ON e.subject_id=a.subject_id AND e.student_user_id=? AND e.enrollment_status='ENROLLED' WHERE a.status='PUBLISHED' AND a.due_at>NOW() ORDER BY a.due_at",me.id());
    }

    @GetMapping("/{id}")
    public Map<String,Object> detail(@PathVariable long id,HttpSession session) {
        Identity me=identity(session,"STUDENT");
        Map<String,Object> a=one("SELECT a.assignment_id,a.title,a.instructions,a.due_at,a.max_score,a.attempt_duration_minutes,a.status,s.subject_name FROM assignments a JOIN subjects s ON s.subject_id=a.subject_id JOIN subject_enrollments e ON e.subject_id=a.subject_id AND e.student_user_id=? AND e.enrollment_status='ENROLLED' WHERE a.assignment_id=? AND a.status='PUBLISHED' AND a.due_at>NOW()",me.id(),id);
        a.put("questions",db.queryForList("SELECT question_id,question_text,question_type,points,position FROM assignment_questions WHERE assignment_id=? ORDER BY position",id)); return a;
    }

    @PostMapping("/{id}/start") @Transactional
    public Map<String,Object> start(@PathVariable long id,HttpSession session) {
        Identity me=identity(session,"STUDENT"); Map<String,Object> a=one("SELECT a.assignment_id,a.attempt_duration_minutes,a.due_at FROM assignments a JOIN subject_enrollments e ON e.subject_id=a.subject_id AND e.student_user_id=? AND e.enrollment_status='ENROLLED' WHERE a.assignment_id=? AND a.status='PUBLISHED'",me.id(),id);
        if(!((java.sql.Timestamp)a.get("due_at")).toLocalDateTime().isAfter(LocalDateTime.now())) throw bad("The submission deadline has passed.");
        List<Map<String,Object>> existing=db.queryForList("SELECT submission_id,attempt_started_at,attempt_expires_at,status FROM assignment_submissions WHERE assignment_id=? AND student_user_id=?",id,me.id());
        long sid; if(existing.isEmpty()) {
            sid=insertAndGetId("INSERT INTO assignment_submissions(assignment_id,student_user_id,status,attempt_started_at,attempt_expires_at) VALUES(?,?,'DRAFT',NOW(),LEAST(DATE_ADD(NOW(),INTERVAL ? MINUTE),?))",id,me.id(),a.get("attempt_duration_minutes"),a.get("due_at"));
        } else { var e=existing.get(0); sid=((Number)e.get("submission_id")).longValue(); if(!"DRAFT".equals(e.get("status").toString())) throw bad("This assignment has already been submitted."); }
        return one("SELECT submission_id,attempt_started_at,attempt_expires_at,NOW() server_time FROM assignment_submissions WHERE submission_id=?",sid);
    }

    @GetMapping("/submissions/{submissionId}")
    public Map<String,Object> attempt(@PathVariable long submissionId,HttpSession session) {
        Identity me=identity(session,"STUDENT");
        Map<String,Object> attempt=one("SELECT s.submission_id,s.assignment_id,s.status,s.attempt_started_at,s.attempt_expires_at,NOW() server_time,a.title,a.due_at,a.max_score FROM assignment_submissions s JOIN assignments a ON a.assignment_id=s.assignment_id WHERE s.submission_id=? AND s.student_user_id=?",submissionId,me.id());
        attempt.put("questions",db.queryForList("SELECT q.question_id,q.question_text,q.question_type,q.points,q.position,aa.answer_text FROM assignment_questions q LEFT JOIN assignment_answers aa ON aa.question_id=q.question_id AND aa.submission_id=? WHERE q.assignment_id=? ORDER BY q.position",submissionId,attempt.get("assignment_id")));
        attempt.put("attachments",db.queryForList("SELECT attachment_id,original_name,size_bytes,uploaded_at FROM assignment_attachments WHERE submission_id=?",submissionId));
        return attempt;
    }

    @PutMapping("/submissions/{submissionId}/answers/{questionId}")
    public Map<String,Object> saveAnswer(@PathVariable long submissionId,@PathVariable long questionId,@RequestBody Map<String,Object> in,HttpSession session) {
        Identity me=identity(session,"STUDENT"); String answer=text(in,"answer",30000,false);
        Map<String,Object> row=one("SELECT s.assignment_id,s.attempt_expires_at,s.status,a.due_at,q.question_id FROM assignment_submissions s JOIN assignments a ON a.assignment_id=s.assignment_id JOIN assignment_questions q ON q.assignment_id=s.assignment_id AND q.question_id=? WHERE s.submission_id=? AND s.student_user_id=?",questionId,submissionId,me.id());
        if(!"DRAFT".equals(row.get("status").toString()) || row.get("attempt_expires_at")==null || !((java.sql.Timestamp)row.get("attempt_expires_at")).toLocalDateTime().isAfter(LocalDateTime.now()) || !((java.sql.Timestamp)row.get("due_at")).toLocalDateTime().isAfter(LocalDateTime.now())) throw bad("This attempt is no longer editable.");
        db.update("INSERT INTO assignment_answers(submission_id,question_id,answer_text) VALUES(?,?,?) ON DUPLICATE KEY UPDATE answer_text=VALUES(answer_text)",submissionId,questionId,answer);
        return Map.of("saved",true);
    }

    @PostMapping("/submissions/{submissionId}/attachments")
    public Map<String,Object> upload(@PathVariable long submissionId,@RequestParam("file") MultipartFile file,HttpSession session) throws IOException {
        Identity me=identity(session,"STUDENT"); Map<String,Object> s=one("SELECT s.status,s.attempt_expires_at,a.due_at FROM assignment_submissions s JOIN assignments a ON a.assignment_id=s.assignment_id WHERE s.submission_id=? AND s.student_user_id=? AND a.status='PUBLISHED' AND s.attempt_expires_at>NOW() AND a.due_at>NOW()",submissionId,me.id());
        if(!"DRAFT".equals(s.get("status").toString()) || file.isEmpty() || file.getSize()>10*1024*1024) throw bad("Choose a file up to 10 MB while your attempt is active.");
        String raw=Objects.requireNonNullElse(file.getOriginalFilename(),"attachment").replace('\\','/');
        String original=raw.substring(raw.lastIndexOf('/')+1);
        String ext=original.contains(".")?original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT):"";
        if(!Set.of(".pdf",".doc",".docx",".txt",".png",".jpg",".jpeg").contains(ext)) throw bad("Allowed files: PDF, DOC, DOCX, TXT, PNG or JPG.");
        if(original.isBlank() || original.length()>180 || original.chars().anyMatch(Character::isISOControl)) throw bad("The filename is invalid.");
        Files.createDirectories(uploads); String stored=UUID.randomUUID()+"_"+original; Path target=uploads.resolve(stored).normalize(); if(!target.startsWith(uploads)) throw bad("Invalid upload path.");
        file.transferTo(target);
        try {
            db.update("INSERT INTO assignment_attachments(submission_id,stored_name,original_name,content_type,size_bytes) VALUES(?,?,?,?,?)",submissionId,stored,original,file.getContentType()==null?"application/octet-stream":file.getContentType(),file.getSize());
        } catch (RuntimeException exception) {
            Files.deleteIfExists(target);
            throw exception;
        }
        return Map.of("uploaded",true,"filename",original);
    }

    @GetMapping("/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable long attachmentId, HttpSession session) throws IOException {
        Identity me = identity(session, "TEACHER");
        Map<String,Object> attachment = one("SELECT at.stored_name,at.original_name,a.created_by "
                + "FROM assignment_attachments at "
                + "JOIN assignment_submissions s ON s.submission_id=at.submission_id "
                + "JOIN assignments a ON a.assignment_id=s.assignment_id "
                + "WHERE at.attachment_id=?", attachmentId);
        if (((Number) attachment.get("created_by")).longValue() != me.id()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Path target = uploads.resolve(attachment.get("stored_name").toString()).normalize();
        if (!target.startsWith(uploads) || !Files.isRegularFile(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        String filename = attachment.get("original_name").toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(Files.size(target))
                .body(new FileSystemResource(target));
    }

    @PostMapping("/submissions/{submissionId}/submit") @Transactional
    public Map<String,Object> submit(@PathVariable long submissionId,HttpSession session) {
        Identity me=identity(session,"STUDENT"); Map<String,Object> s=one("SELECT s.assignment_id,s.status,s.attempt_expires_at,a.due_at,a.status assignment_status FROM assignment_submissions s JOIN assignments a ON a.assignment_id=s.assignment_id WHERE s.submission_id=? AND s.student_user_id=?",submissionId,me.id());
        if(!"DRAFT".equals(s.get("status").toString())) throw bad("This submission is already final.");
        if(!"PUBLISHED".equals(s.get("assignment_status").toString())) throw bad("This assignment is no longer open.");
        db.update("UPDATE assignment_submissions SET status='SUBMITTED',submitted_at=NOW(),evaluation_status='PROCESSING' WHERE submission_id=?",submissionId);
        try {
            db.update("INSERT INTO assignment_answers(submission_id,question_id,answer_text) "
                    + "SELECT ?,q.question_id,'' FROM assignment_questions q "
                    + "LEFT JOIN assignment_answers aa ON aa.submission_id=? AND aa.question_id=q.question_id "
                    + "WHERE q.assignment_id=? AND aa.answer_id IS NULL",submissionId,submissionId,s.get("assignment_id"));
            List<Map<String,Object>> answers=db.queryForList("SELECT aa.answer_id,aa.answer_text,q.question_text,q.question_type,q.model_answer,q.rubric,q.points FROM assignment_answers aa JOIN assignment_questions q ON q.question_id=aa.question_id WHERE aa.submission_id=?",submissionId);
            for(var ans:answers){
                Map<String,Object> evaluation=ai.evaluate(ans.get("question_text").toString(),ans.get("question_type").toString(),(BigDecimal)ans.get("points"),Objects.toString(ans.get("model_answer"),""),Objects.toString(ans.get("rubric"),""),Objects.toString(ans.get("answer_text"),""));
                db.update("UPDATE assignment_answers SET ai_score=?,ai_feedback=? WHERE answer_id=?",evaluation.get("score"),evaluation.get("feedback"),ans.get("answer_id"));
            }
            db.update("UPDATE assignment_submissions SET evaluation_status='EVALUATED' WHERE submission_id=?",submissionId);
        } catch(Exception e) { db.update("UPDATE assignment_submissions SET evaluation_status='FAILED' WHERE submission_id=?",submissionId); }
        return one("SELECT submission_id,status,evaluation_status,submitted_at FROM assignment_submissions WHERE submission_id=?",submissionId);
    }

    @GetMapping("/teacher/submissions")
    public List<Map<String,Object>> teacherSubmissions(HttpSession session) {
        Identity me=identity(session,"TEACHER");
        try { return db.queryForList("SELECT s.submission_id,a.assignment_id,a.title,u.full_name student,s.status,s.evaluation_status,s.submitted_at,s.score FROM assignment_submissions s JOIN assignments a ON a.assignment_id=s.assignment_id JOIN users u ON u.user_id=s.student_user_id WHERE a.created_by=? ORDER BY s.submitted_at DESC",me.id()); }
        catch (BadSqlGrammarException e) { throw migrationRequired(e); }
    }

    @PostMapping("/{assignmentId}/submissions/{submissionId}/evaluate")
    public Map<String,Object> retryEvaluation(@PathVariable long assignmentId,@PathVariable long submissionId,HttpSession session) {
        Identity me=identity(session,"TEACHER"); requireTeacherOwns(assignmentId,me.id());
        one("SELECT submission_id FROM assignment_submissions WHERE submission_id=? AND assignment_id=? AND status IN ('SUBMITTED','GRADED')",submissionId,assignmentId);
        db.update("UPDATE assignment_submissions SET evaluation_status='PROCESSING' WHERE submission_id=?",submissionId);
        try {
            List<Map<String,Object>> answers=db.queryForList("SELECT aa.answer_id,aa.answer_text,q.question_text,q.question_type,q.model_answer,q.rubric,q.points FROM assignment_answers aa JOIN assignment_questions q ON q.question_id=aa.question_id WHERE aa.submission_id=?",submissionId);
            for(var ans:answers){var ev=ai.evaluate(ans.get("question_text").toString(),ans.get("question_type").toString(),(BigDecimal)ans.get("points"),Objects.toString(ans.get("model_answer"),""),Objects.toString(ans.get("rubric"),""),Objects.toString(ans.get("answer_text"),""));db.update("UPDATE assignment_answers SET ai_score=?,ai_feedback=? WHERE answer_id=?",ev.get("score"),ev.get("feedback"),ans.get("answer_id"));}
            db.update("UPDATE assignment_submissions SET evaluation_status='EVALUATED' WHERE submission_id=?",submissionId);
        } catch(Exception e) { db.update("UPDATE assignment_submissions SET evaluation_status='FAILED' WHERE submission_id=?",submissionId); throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"AI evaluation failed. Retry evaluation."); }
        return Map.of("evaluationStatus","EVALUATED");
    }

    @GetMapping("/{assignmentId}/submissions/{submissionId}/review")
    public Map<String,Object> review(@PathVariable long assignmentId,@PathVariable long submissionId,HttpSession session) {
        Identity me=identity(session,"TEACHER"); requireTeacherOwns(assignmentId,me.id());
        Map<String,Object> s=one("SELECT s.submission_id,s.score,s.feedback,s.status,s.evaluation_status,u.full_name student,a.title assignment FROM assignment_submissions s JOIN users u ON u.user_id=s.student_user_id JOIN assignments a ON a.assignment_id=s.assignment_id WHERE s.submission_id=? AND s.assignment_id=?",submissionId,assignmentId);
        s.put("answers",db.queryForList("SELECT q.question_id,q.question_text,q.model_answer,q.rubric,q.points,aa.answer_text,aa.ai_score,aa.ai_feedback,aa.final_score,aa.final_feedback FROM assignment_answers aa JOIN assignment_questions q ON q.question_id=aa.question_id WHERE aa.submission_id=? ORDER BY q.position",submissionId));
        s.put("attachments",db.queryForList("SELECT attachment_id,original_name,content_type,size_bytes,uploaded_at FROM assignment_attachments WHERE submission_id=?",submissionId));return s;
    }

    @PutMapping("/{assignmentId}/submissions/{submissionId}/answers/{questionId}/grade")
    public Map<String,Object> grade(@PathVariable long assignmentId,@PathVariable long submissionId,@PathVariable long questionId,@RequestBody Map<String,Object> in,HttpSession session) {
        Identity me=identity(session,"TEACHER"); requireTeacherOwns(assignmentId,me.id()); BigDecimal score=decimal(in,"score"); String feedback=text(in,"feedback",10000,false);
        Map<String,Object> q=one("SELECT q.points FROM assignment_questions q JOIN assignment_answers aa ON aa.question_id=q.question_id JOIN assignment_submissions s ON s.submission_id=aa.submission_id WHERE q.question_id=? AND s.submission_id=? AND s.assignment_id=?",questionId,submissionId,assignmentId);
        if(score.signum()<0||score.compareTo((BigDecimal)q.get("points"))>0)throw bad("Final score must be between zero and the question maximum.");
        db.update("UPDATE assignment_answers SET final_score=?,final_feedback=?,evaluated_by=?,evaluated_at=NOW() WHERE question_id=? AND submission_id=?",score,feedback,me.id(),questionId,submissionId);
        Integer ungraded = db.queryForObject("SELECT COUNT(*) FROM assignment_answers WHERE submission_id=? AND final_score IS NULL", Integer.class, submissionId);
        if (ungraded == null || ungraded == 0) {
            db.update("UPDATE assignment_submissions s SET score=(SELECT SUM(final_score) FROM assignment_answers aa WHERE aa.submission_id=s.submission_id),feedback='Teacher reviewed',graded_by=?,graded_at=NOW(),status='GRADED' WHERE submission_id=?",me.id(),submissionId);
        } else {
            db.update("UPDATE assignment_submissions SET score=(SELECT SUM(COALESCE(final_score,ai_score,0)) FROM assignment_answers WHERE submission_id=?) WHERE submission_id=?",submissionId,submissionId);
        }
        return Map.of("saved",true,"finalized",ungraded == null || ungraded == 0);
    }

    private static String text(Map<String,Object> m,String key,int max,boolean required){Object v=m.get(key);String s=v==null?"":v.toString().trim();if((required&&s.isEmpty())||s.length()>max)throw bad("Check the " + key + " field.");return s;}
    private long insertAndGetId(String sql, Object... values) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        db.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            return statement;
        }, keyHolder);
        Number generatedId = keyHolder.getKey();
        if (generatedId == null) throw new IllegalStateException("The database did not return a generated assignment ID.");
        return generatedId.longValue();
    }
    private static int integer(Map<String,Object> m,String key,int min,int max){try{int v=Integer.parseInt(m.get(key).toString());if(v<min||v>max)throw new NumberFormatException();return v;}catch(Exception e){throw bad("Invalid " + key + ".");}}
    private static int optionalInteger(Map<String,Object> m,String key,int min,int max){if(m.get(key)==null||m.get(key).toString().isBlank())return 0;return integer(m,key,min,max);}
    private static BigDecimal decimal(Map<String,Object> m,String key){try{return new BigDecimal(m.get(key).toString());}catch(Exception e){throw bad("Invalid " + key + ".");}}
    private static ResponseStatusException bad(String s){return new ResponseStatusException(HttpStatus.BAD_REQUEST,s);}
    private static ResponseStatusException migrationRequired(BadSqlGrammarException cause) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Database migration V4 is not applied. Run database/migrations/V4__assignment_attempts_and_attachments.sql against the OpenStudy database, then restart the backend.", cause);
    }
}
