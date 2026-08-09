package org.hackathon.security.sdupass;

public record SduPassResult(
        Integer code,
        String msg,
        Object data,
        Long timestamp
) {

    public static SduPassResult ok() {
        return new SduPassResult(200, "success", null, System.currentTimeMillis());
    }

    public static SduPassResult success(Object data) {
        return new SduPassResult(200, "success", data, System.currentTimeMillis());
    }

}
