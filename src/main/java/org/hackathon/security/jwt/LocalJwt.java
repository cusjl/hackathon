package org.hackathon.security.jwt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocalJwt {
    private Integer userId;
    private String name;
    private Boolean studentFlag;
    private String casId;

    public enum Type {
        ACCESS,
        EXCHANGE,
        REGISTER

    }

    public enum Auth {
        OK,
        ANONYMOUS,
        EXPIRED,
        INVALID
    }

    public record Resolved(LocalJwt jwt, Auth auth) {
        public static final Resolved ANONYMOUS = new Resolved(null, Auth.ANONYMOUS);
        public static final Resolved EXPIRED = new Resolved(null, Auth.EXPIRED);
        public static final Resolved INVALID = new Resolved(null, Auth.INVALID);
        public static Resolved ok(LocalJwt jwt)  { return new Resolved(jwt, Auth.OK); }
    }
}
