package org.hackathon.data.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LoginVO {
    private String token;
    /** 平台内稳定用户标识，可与 TeamMemberVO.userId 对比。 */
    private Integer userId;
    private String name;
    private Boolean studentFlag;
    /** 学生的真实统一认证学号；外部账号为 null。 */
    private String casId;
    /** 当前账号绑定的邮箱，与 casId 分开返回。 */
    private String email;
    private List<AuthorityEventVO> authorities;
}
