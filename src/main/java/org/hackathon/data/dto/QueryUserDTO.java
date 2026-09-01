package org.hackathon.data.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QueryUserDTO {
    //精确匹配
    Integer userId;
    //包含匹配
    String name;
    //包含匹配（姓名查询的前端字段别名）
    String userName;
    //前缀匹配
    @Size(max = 11, message = "手机号不能超过11位")
    String phone;
    //前缀匹配
    String email;
    //前缀匹配
    @Size(max = 12, message = "学号不能超过12位")
    String casId;
    @JsonSetter(nulls = Nulls.SKIP)
    Boolean onlyStudent = false;
    @JsonSetter(nulls = Nulls.SKIP)
    Boolean onlyExtern = false;
    @JsonSetter(nulls = Nulls.SKIP)
    Boolean onlySuper = false;
}
