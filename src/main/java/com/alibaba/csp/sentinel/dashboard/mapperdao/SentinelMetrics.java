package com.alibaba.csp.sentinel.dashboard.mapperdao;

import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

/**
 * @author 王智勇
 * @date 2025年04月28日 16:56
 */
@Table(name = "t_sentinel_metric")
public class SentinelMetrics {
    @Id
    private Long id;                    // id，主键
    private Date gmtCreate;             // 创建时间
    private Date gmtModified;           // 修改时间
    private String app;                 // 应用名称
    private Date timestamp;             // 统计时间
    private String resource;            // 资源名称
    private Long passQps;            // 通过qps
    private Long successQps;         // 成功qps
    private Long blockQps;           // 限流qps
    private Long exceptionQps;       // 发送异常的次数
    private Double rt;                  // 所有successQps的rt的和
    private Integer count;              // 本次聚合的总条数
    private Integer resourceCode;       // 资源的hashCode

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public Long getPassQps() {
        return passQps;
    }

    public void setPassQps(Long passQps) {
        this.passQps = passQps;
    }

    public Long getSuccessQps() {
        return successQps;
    }

    public void setSuccessQps(Long successQps) {
        this.successQps = successQps;
    }

    public Long getBlockQps() {
        return blockQps;
    }

    public void setBlockQps(Long blockQps) {
        this.blockQps = blockQps;
    }

    public Long getExceptionQps() {
        return exceptionQps;
    }

    public void setExceptionQps(Long exceptionQps) {
        this.exceptionQps = exceptionQps;
    }

    public Double getRt() {
        return rt;
    }

    public void setRt(Double rt) {
        this.rt = rt;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Integer getResourceCode() {
        return resourceCode;
    }

    public void setResourceCode(Integer resourceCode) {
        this.resourceCode = resourceCode;
    }
}
