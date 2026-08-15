package org.hackathon.data.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ResultCode {

    //1XXX 账号与鉴权
    //10XX  鉴权相关
    //100X   全局权限
    UNAUTHORIZED(1000, HttpStatus.UNAUTHORIZED, "用户无权限"),
    TOKEN_UNREADABLE(1001, HttpStatus.UNAUTHORIZED, "token校验失败"),
    TOKEN_EXPIRED(1002, HttpStatus.UNAUTHORIZED, "token过期"),
    NOT_ENROLLED(1003, HttpStatus.UNAUTHORIZED, "学生尚未注册"),
    TOKEN_IS_BLANK(1004, HttpStatus.UNAUTHORIZED, "未携带token"),
    NOT_STUDENT(1005, HttpStatus.UNAUTHORIZED, "用户无校内权限"),
    NOT_SUPER(1006, HttpStatus.UNAUTHORIZED, "用户无超管权限"),
    NOT_EXTERN(1007, HttpStatus.UNAUTHORIZED, "用户不是外部用户"),
    NOT_ON_CAMPUS(1008, HttpStatus.UNAUTHORIZED, "用户不能转为学生"),
    //101X   赛事权限
    NOT_ADMIN(1011, HttpStatus.UNAUTHORIZED, "用户无管理员权限"),
    NOT_JUDGE(1012, HttpStatus.UNAUTHORIZED, "用户无评委权限"),
    NOT_LEADER(1013, HttpStatus.UNAUTHORIZED, "选手无队长权限"),
    //102X   权限设置
    AUTHORITY_REPEAT(1021, HttpStatus.CONFLICT, "重复授权"),
    LAST_SUPER(1022, HttpStatus.CONFLICT, "最后一名超管无法删除"),
    LAST_ADMIN(1023, HttpStatus.CONFLICT, "最后一名赛管无法删除"),
    //11XX  账号相关
    //110X   通用
    USER_NOT_EXIST(1101, HttpStatus.NOT_FOUND, "用户不存在"),
    PHONE_CONFLICT(1102, HttpStatus.CONFLICT, "手机号已被占用"),
    EMAIL_CONFLICT(1103, HttpStatus.CONFLICT, "邮箱已被占用"),
    PASSWORD_INCORRECT(1104, HttpStatus.UNAUTHORIZED, "密码错误"),
    //111X   学生账号
    ALREADY_ENROLLED(1111, HttpStatus.CONFLICT, "学生已注册"),
    TAG_UNAVAILABLE(1112, HttpStatus.CONFLICT, "包含不可用的标签"),
    STUDENT_NOT_EXIST(1113, HttpStatus.NOT_FOUND, "学生不存在"),
    PASSWORD_UNSET(1114, HttpStatus.UNAUTHORIZED, "密码暂未设置"),
    //112X   外部账号
    EX_USER_NOT_EXIST(1121, HttpStatus.NOT_FOUND, "外部用户不存在"),

    //2XXX

    //3XXX 赛事相关
    //30XX  赛事架构
    //300X   赛事层次
    EVENT_NOT_FOUND(3001, HttpStatus.NOT_FOUND, "赛事不存在"),
    INVALID_EVENT_TIME(3002, HttpStatus.CONFLICT, "赛事时间非法"),
    EVENT_ALREADY_REG(3003, HttpStatus.FORBIDDEN, "赛事已开始报名"),
    EVENT_ALREADY_END(3004, HttpStatus.FORBIDDEN, "赛事已经结束"),
    EVENT_ALREADY_EXIST(3005, HttpStatus.CONFLICT, "赛事已存在"),
    //301X   赛道层次
    TRACK_NOT_FOUND(3011, HttpStatus.NOT_FOUND, "赛道不存在"),
    TRACK_ALREADY_EXIST(3012, HttpStatus.CONFLICT, "赛道已存在"),
    BINDING_TRACK(3013, HttpStatus.CONFLICT, "存在关联赛道"),
    //302X   轮次层次
    PHASE_NOT_FOUND(3021, HttpStatus.NOT_FOUND, "轮次不存在"),
    PHASE_EVENT_TIME_CONFLICT(3022, HttpStatus.CONFLICT, "轮次时间与赛事冲突"),
    INVALID_PHASE_TIME(3023, HttpStatus.CONFLICT, "轮次时间非法"),
    PHASE_TIME_CONFLICT(3024, HttpStatus.CONFLICT, "与其他轮次时间冲突"),
    PHASE_ALREADY_EXIST(3025, HttpStatus.CONFLICT, "轮次已存在"),
    BINDING_PHASE(3026, HttpStatus.CONFLICT, "存在关联轮次"),
    PHASE_ALREADY_SUBMIT(3027, HttpStatus.CONFLICT, "轮次已开始提交"),
    //31XX  报名及组队
    //310X   报名
    NOT_REGISTER_TIME(3101, HttpStatus.FORBIDDEN, "赛事未开放报名"),
    REGISTRATION_REPEAT(3102, HttpStatus.CONFLICT, "已报名该赛事"),
    ALREADY_TEAMED(3103, HttpStatus.CONFLICT, "请先退出队伍"),
    NOT_REGISTERED(3104, HttpStatus.CONFLICT, "暂未报名该赛道"),
    //311X   组队
    EVENT_ALREADY_LIVE(3111, HttpStatus.CONFLICT, "赛事已开始"),
    TEAM_NAME_CONFLICT(3112, HttpStatus.CONFLICT, "队伍名已存在"),
    TEAM_NOT_FOUND(3113, HttpStatus.NOT_FOUND, "队伍不存在"),
    TEAM_TYPE_CONFLICT(3114, HttpStatus.CONFLICT, "与队伍协同标签冲突"),
    TEAM_ALREADY_FULL(3115, HttpStatus.CONFLICT, "队伍已满员"),
    LEADER_LEAVING(3116, HttpStatus.CONFLICT, "队长不能离队"),
    NOT_TEAM_MEMBER(3117, HttpStatus.CONFLICT, "选手不在队伍中"),


    //4XXX 通用（全局异常处理）
    PARAM_ERROR(4000, HttpStatus.BAD_REQUEST, "参数错误"),
    RESOURCE_CONFLICT(4001, HttpStatus.CONFLICT, "资源冲突"),
    RESOURCE_UPDATED(4002, HttpStatus.CONFLICT, "存在已更新资源，请刷新重试"),
    PATH_NOT_FOUND(4003, HttpStatus.NOT_FOUND, "该路径不存在资源"),

    INTERNAL_ERROR(5000, HttpStatus.INTERNAL_SERVER_ERROR, "系统内部错误"),
    ;

    private final Integer code;         //业务码
    private final HttpStatus status;
    private final String msg;

}
