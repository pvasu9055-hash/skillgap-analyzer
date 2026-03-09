package com.skillgap.skillgap.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
public class SkillGapController {

    // ─── Learning Resources ────────────────────────────────────────────────────

    private static final Map<String, String> LEARNING_RESOURCES = new HashMap<>() {{
        put("java",            "https://docs.oracle.com/en/java/");
        put("spring",          "https://spring.io/guides");
        put("spring boot",     "https://spring.io/projects/spring-boot");
        put("python",          "https://docs.python.org/3/tutorial/");
        put("javascript",      "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide");
        put("typescript",      "https://www.typescriptlang.org/docs/");
        put("react",           "https://react.dev/learn");
        put("angular",         "https://angular.io/start");
        put("vue",             "https://vuejs.org/guide/introduction.html");
        put("node",            "https://nodejs.org/en/learn");
        put("nodejs",          "https://nodejs.org/en/learn");
        put("sql",             "https://www.w3schools.com/sql/");
        put("mysql",           "https://dev.mysql.com/doc/");
        put("postgresql",      "https://www.postgresql.org/docs/");
        put("mongodb",         "https://www.mongodb.com/docs/manual/");
        put("docker",          "https://docs.docker.com/get-started/");
        put("kubernetes",      "https://kubernetes.io/docs/tutorials/");
        put("aws",             "https://aws.amazon.com/training/");
        put("azure",           "https://learn.microsoft.com/en-us/azure/");
        put("git",             "https://git-scm.com/book/en/v2");
        put("linux",           "https://linuxjourney.com/");
        put("machine learning","https://www.coursera.org/learn/machine-learning");
        put("deep learning",   "https://www.deeplearning.ai/");
        put("data structures", "https://www.coursera.org/learn/data-structures");
        put("algorithms",      "https://www.coursera.org/learn/algorithms-part1");
        put("rest api",        "https://restfulapi.net/");
        put("graphql",         "https://graphql.org/learn/");
        put("redis",           "https://redis.io/docs/");
        put("kafka",           "https://kafka.apache.org/documentation/");
        put("microservices",   "https://microservices.io/");
    }};

    // ─── Synonym / Alias Groups ────────────────────────────────────────────────

    private static final List<Set<String>> SYNONYM_GROUPS = List.of(
            new HashSet<>(Arrays.asList("js", "javascript")),
            new HashSet<>(Arrays.asList("ts", "typescript")),
            new HashSet<>(Arrays.asList("node", "nodejs", "node.js")),
            new HashSet<>(Arrays.asList("react", "reactjs", "react.js")),
            new HashSet<>(Arrays.asList("vue", "vuejs", "vue.js")),
            new HashSet<>(Arrays.asList("postgres", "postgresql")),
            new HashSet<>(Arrays.asList("mongo", "mongodb")),
            new HashSet<>(Arrays.asList("k8s", "kubernetes")),
            new HashSet<>(Arrays.asList("ml", "machine learning")),
            new HashSet<>(Arrays.asList("dl", "deep learning")),
            new HashSet<>(Arrays.asList("spring", "spring boot", "springboot"))
    );

    // ─── Endpoint ──────────────────────────────────────────────────────────────

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeSkills(@RequestBody Map<String, String> skills) {

        // ── Input Validation ──────────────────────────────────────────────────
        String studentRaw = skills.get("studentSkills");
        String jobRaw     = skills.get("jobSkills");

        List<String> errors = new ArrayList<>();
        if (studentRaw == null || studentRaw.isBlank())
            errors.add("'studentSkills' is required and cannot be empty.");
        if (jobRaw == null || jobRaw.isBlank())
            errors.add("'jobSkills' is required and cannot be empty.");
        if (!errors.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("errors", errors));

        List<String> studentSkills = parseSkills(studentRaw);
        List<String> jobSkills     = parseSkills(jobRaw);

        if (studentSkills.isEmpty())
            return ResponseEntity.badRequest()
                    .body(Map.of("errors", List.of("'studentSkills' contains no valid entries.")));
        if (jobSkills.isEmpty())
            return ResponseEntity.badRequest()
                    .body(Map.of("errors", List.of("'jobSkills' contains no valid entries.")));

        // ── Analysis ──────────────────────────────────────────────────────────
        List<SkillResult> missingSkills  = new ArrayList<>();
        List<SkillResult> matchedSkills  = new ArrayList<>();
        List<SkillResult> fuzzyMatches   = new ArrayList<>();

        for (String jobSkill : jobSkills) {
            if (isExactMatch(jobSkill, studentSkills)) {
                matchedSkills.add(new SkillResult(jobSkill, "exact", null, null));

            } else {
                String fuzzy = findFuzzyMatch(jobSkill, studentSkills);
                if (fuzzy != null) {
                    fuzzyMatches.add(new SkillResult(jobSkill, "fuzzy", fuzzy, getResource(jobSkill)));
                } else {
                    missingSkills.add(new SkillResult(jobSkill, "missing", null, getResource(jobSkill)));
                }
            }
        }

        // ── Scoring ───────────────────────────────────────────────────────────
        int total        = jobSkills.size();
        int exactCount   = matchedSkills.size();
        int fuzzyCount   = fuzzyMatches.size();
        int missingCount = missingSkills.size();

        // Fuzzy matches count as half a point
        double score        = (exactCount + fuzzyCount * 0.5) / total * 100.0;
        double roundedScore = Math.round(score * 10.0) / 10.0;

        String readiness;
        if      (score >= 85) readiness = "Strong Match";
        else if (score >= 60) readiness = "Good Candidate – Some Gaps";
        else if (score >= 35) readiness = "Moderate Match – Significant Gaps";
        else                  readiness = "Early Stage – Major Upskilling Needed";

        // ── Response ──────────────────────────────────────────────────────────
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", Map.of(
                "totalJobSkills",    total,
                "exactMatches",      exactCount,
                "fuzzyMatches",      fuzzyCount,
                "missingSkills",     missingCount,
                "matchScorePercent", roundedScore,
                "readinessLevel",    readiness
        ));
        response.put("matched",      matchedSkills);
        response.put("fuzzyMatched", fuzzyMatches);
        response.put("missing",      missingSkills);

        return ResponseEntity.ok(response);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    /** Split on commas, trim, lowercase, drop blank entries. */
    private List<String> parseSkills(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    /** True if jobSkill matches any student skill exactly or via synonym. */
    private boolean isExactMatch(String jobSkill, List<String> studentSkills) {
        if (studentSkills.contains(jobSkill)) return true;
        Set<String> synonyms = getSynonyms(jobSkill);
        return studentSkills.stream().anyMatch(synonyms::contains);
    }

    /**
     * Returns the best-matching student skill if it is "close enough" to the
     * job skill, otherwise null.  Uses Levenshtein distance ≤ 2 or substring
     * containment (e.g., "react" matches "reactjs").
     */
    private String findFuzzyMatch(String jobSkill, List<String> studentSkills) {
        for (String s : studentSkills) {
            if (s.contains(jobSkill) || jobSkill.contains(s)) return s;
            if (levenshtein(jobSkill, s) <= 2)                 return s;
        }
        return null;
    }

    /** Expand a skill to its synonym group (always includes itself). */
    private Set<String> getSynonyms(String skill) {
        for (Set<String> group : SYNONYM_GROUPS) {
            if (group.contains(skill)) return group;
        }
        return Set.of(skill);
    }

    /** Look up a learning resource URL, checking synonyms too. */
    private String getResource(String skill) {
        if (LEARNING_RESOURCES.containsKey(skill)) return LEARNING_RESOURCES.get(skill);
        for (String synonym : getSynonyms(skill)) {
            if (LEARNING_RESOURCES.containsKey(synonym))
                return LEARNING_RESOURCES.get(synonym);
        }
        // Fallback: generic Google search
        return "https://www.google.com/search?q=learn+" + skill.replace(" ", "+");
    }

    /** Standard iterative Levenshtein distance. */
    private int levenshtein(String a, String b) {
        int m = a.length(), n = b.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        for (int i = 1; i <= m; i++)
            for (int j = 1; j <= n; j++)
                dp[i][j] = a.charAt(i-1) == b.charAt(j-1)
                        ? dp[i-1][j-1]
                        : 1 + Math.min(dp[i-1][j-1], Math.min(dp[i-1][j], dp[i][j-1]));
        return dp[m][n];
    }

    // ─── Inner Record ──────────────────────────────────────────────────────────

    /** Represents a single skill in the analysis result. */
    static class SkillResult {
        public final String skill;
        public final String matchType;       // "exact" | "fuzzy" | "missing"
        public final String matchedWith;     // populated for fuzzy matches
        public final String learningResource;

        SkillResult(String skill, String matchType, String matchedWith, String learningResource) {
            this.skill            = skill;
            this.matchType        = matchType;
            this.matchedWith      = matchedWith;
            this.learningResource = learningResource;
        }
    }
}