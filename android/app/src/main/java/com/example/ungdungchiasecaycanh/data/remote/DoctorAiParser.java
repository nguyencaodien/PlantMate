package com.example.ungdungchiasecaycanh.data.remote;

import com.example.ungdungchiasecaycanh.model.PlantDiagnosisResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Turns the model's JSON answer into a {@link PlantDiagnosisResult}. Tolerates code fences,
 * booleans sent as strings and confidences sent as percentages; rejects photos that are not
 * plants and answers without a diagnosis or care steps.
 */
public final class DoctorAiParser {
    public static final int MAX_DIAGNOSIS = 600;
    static final int MAX_GUIDANCE = 2000;
    static final int MAX_STEPS = 6;

    private DoctorAiParser() { }

    public static PlantDiagnosisResult parse(String text) throws DoctorAiException {
        JSONObject json;
        try {
            json = new JSONObject(stripFences(text));
        } catch (JSONException | NullPointerException error) {
            throw new DoctorAiException(DoctorAiException.Kind.FAILED, "Unreadable answer", error);
        }
        if (Boolean.FALSE.equals(bool(json, "isPlant"))) {
            throw new DoctorAiException(DoctorAiException.Kind.NOT_A_PLANT, "Not a plant");
        }
        String diagnosis = cut(json.optString("diagnosis", ""), MAX_DIAGNOSIS);
        if (diagnosis.isEmpty()) {
            throw new DoctorAiException(DoctorAiException.Kind.FAILED, "Missing diagnosis");
        }
        String guidance = guidance(json);
        if (guidance.isEmpty()) {
            throw new DoctorAiException(DoctorAiException.Kind.FAILED, "Missing guidance");
        }
        return new PlantDiagnosisResult(diagnosis, confidence(json), guidance,
                blankToNull(json.optString("commonName", "")),
                blankToNull(json.optString("scientificName", "")), bool(json, "healthy"));
    }

    /** Care steps joined one per line (the result screen numbers them), within the length limit. */
    private static String guidance(JSONObject json) {
        JSONArray steps = json.optJSONArray("careSteps");
        StringBuilder out = new StringBuilder();
        if (steps != null) {
            for (int i = 0; i < steps.length() && i < MAX_STEPS; i++) {
                String step = steps.optString(i, "").trim().replaceAll("\\s+", " ");
                if (step.isEmpty()) continue;
                if (out.length() + step.length() + 1 > MAX_GUIDANCE) break;
                if (out.length() > 0) out.append('\n');
                out.append(step);
            }
        }
        if (out.length() == 0) return cut(json.optString("guidance", ""), MAX_GUIDANCE);
        return out.toString();
    }

    private static Double confidence(JSONObject json) {
        if (!json.has("confidence") || json.isNull("confidence")) return null;
        double value = json.optDouble("confidence", Double.NaN);
        if (Double.isNaN(value) || value < 0) return null;
        if (value > 1 && value <= 100) value = value / 100d;
        return value > 1 ? null : value;
    }

    private static Boolean bool(JSONObject json, String key) {
        Object value = json.opt(key);
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) {
            String s = ((String) value).trim().toLowerCase(Locale.ROOT);
            if (s.equals("true") || s.equals("yes")) return true;
            if (s.equals("false") || s.equals("no")) return false;
        }
        return null;
    }

    static String stripFences(String text) {
        String t = text.trim();
        if (t.startsWith("```")) {
            int firstLine = t.indexOf('\n');
            int end = t.lastIndexOf("```");
            if (firstLine > 0 && end > firstLine) t = t.substring(firstLine + 1, end).trim();
        }
        return t;
    }

    private static String cut(String value, int max) {
        String t = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        return t.length() > max ? t.substring(0, max - 1).trim() + "…" : t;
    }

    private static String blankToNull(String value) {
        String t = value == null ? "" : value.trim();
        return t.isEmpty() ? null : t;
    }
}
