package org.hackathon.data.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmissionIdVO {
    private Integer submissionId;
    //本次提交生成的版本号
    private Integer versionNo;
}
