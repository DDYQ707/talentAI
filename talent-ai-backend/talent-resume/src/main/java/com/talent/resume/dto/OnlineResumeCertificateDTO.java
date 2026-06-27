package com.talent.resume.dto;

import lombok.Data;

@Data
public class OnlineResumeCertificateDTO {

    private Long id;

    /** 1-证书 2-荣誉 3-职称 */
    private Integer certType;

    private String name;

    private String issuer;

    private String issueDate;

    private String description;

    private Integer sortOrder;
}
