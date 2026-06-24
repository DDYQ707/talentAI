package com.talent.job.controller;

import com.talent.job.dto.JobPostUpdateRequest;
import com.talent.job.entity.JobPost;
import com.talent.job.service.IJobPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import com.talent.job.feign.AuthFeignClient;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/job") // 注意：确保你的网关路由配置能匹配到这个前缀，比如 /api/job/** 路由到 talent-job
public class JobPostController {

    @Autowired
    private IJobPostService jobPostService;

    @Autowired
    private AuthFeignClient authFeignClient;
    @PostMapping("/publish")
    public Map<String, Object> publishJob(
            @RequestBody JobPost jobPost,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {

        Map<String, Object> result = new HashMap<>();

        if (userId == null) {
            result.put("code", 401);
            result.put("msg", "未检测到登录用户信息！");
            return result;
        }

        jobPost.setPublisherId(userId);

        // ==========================================
        // 🔥 见证奇迹：通过 OpenFeign 远程呼叫 talent-auth 获取真实姓名！
        // ==========================================
        String realName = authFeignClient.getUserName(userId);
        System.out.println("通过 OpenFeign 获取到的真实姓名是：" + realName);

        jobPost.setPublisherName(realName); // <--- 填入真实的姓名

        if (jobPost.getDeptId() == null) {
            jobPost.setDeptId(1L);
        }
        if (!StringUtils.hasText(jobPost.getDeptName())) {
            jobPost.setDeptName("技术部");
        }
        if (jobPost.getEmploymentType() == null) {
            jobPost.setEmploymentType((byte) 1);
        }
        if (jobPost.getHeadcount() == null) {
            jobPost.setHeadcount(1);
        }
        if (jobPost.getPriority() == null) {
            jobPost.setPriority((byte) 2);
        }
        jobPost.setStatus((byte) 1);
        jobPost.setPublishedAt(LocalDateTime.now());

        boolean success = jobPostService.save(jobPost);

        if (success) {
            result.put("code", 200);
            result.put("msg", "岗位发布成功！");
            result.put("data", jobPost);
        } else {
            result.put("code", 500);
            result.put("msg", "数据库写入失败");
        }

        return result;
    }
    @GetMapping("/list")
    public Map<String, Object> getJobList(
            @RequestParam(value = "current", defaultValue = "1") Integer current, // 当前页码，默认第1页
            @RequestParam(value = "size", defaultValue = "10") Integer size,      // 每页条数，默认10条
            @RequestParam(value = "title", required = false) String title,        // 兼容：岗位名称模糊搜索
            @RequestParam(value = "keyword", required = false) String keyword,    // 关键词：岗位/部门/城市/技能等模糊匹配
            @RequestParam(value = "deptNames", required = false) String deptNames, // 部门名称精确匹配，逗号分隔
            @RequestParam(value = "status", required = false) Byte status         // 可选：根据状态过滤 (1-招聘中)
    ) {
        Map<String, Object> result = new HashMap<>();

        // 1. 构造分页对象
        Page<JobPost> pageParam = new Page<>(current, size);

        // 2. 构造查询条件
        LambdaQueryWrapper<JobPost> queryWrapper = new LambdaQueryWrapper<>();

        String searchText = StringUtils.hasText(keyword) ? keyword.trim() : (StringUtils.hasText(title) ? title.trim() : null);
        if (StringUtils.hasText(searchText)) {
            queryWrapper.and(w -> w.like(JobPost::getTitle, searchText)
                    .or()
                    .like(JobPost::getDeptName, searchText)
                    .or()
                    .like(JobPost::getWorkCity, searchText)
                    .or()
                    .like(JobPost::getSkillTags, searchText)
                    .or()
                    .like(JobPost::getJobDescription, searchText)
                    .or()
                    .like(JobPost::getJobRequirements, searchText));
        }

        if (StringUtils.hasText(deptNames)) {
            List<String> deptNameList = Arrays.stream(deptNames.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .toList();
            if (!deptNameList.isEmpty()) {
                queryWrapper.in(JobPost::getDeptName, deptNameList);
            }
        }

        // 如果前端传了 status 参数，就精确匹配
        if (status != null) {
            queryWrapper.eq(JobPost::getStatus, status);
        }

        // 按照创建时间倒序排列 (最新的岗位排在最前面)
        queryWrapper.orderByDesc(JobPost::getCreatedAt);

        // 3. 执行分页查询 (MyBatis-Plus 会自动把总数和当前页的数据装入 pageResult 中)
        Page<JobPost> pageResult = jobPostService.page(pageParam, queryWrapper);

        // 4. 封装返回结果
        result.put("code", 200);
        result.put("msg", "查询成功");

        Map<String, Object> data = new HashMap<>();
        data.put("total", pageResult.getTotal());       // 总条数
        data.put("current", pageResult.getCurrent());   // 当前页码
        data.put("pages", pageResult.getPages());       // 总页数
        data.put("records", pageResult.getRecords());   // 当前页的真实数据列表

        result.put("data", data);

        return result;
    }

    @GetMapping("/detail")
    public Map<String, Object> getJobDetail(@RequestParam("id") Long id) {
        Map<String, Object> result = new HashMap<>();

        if (id == null) {
            result.put("code", 400);
            result.put("msg", "岗位 ID 不能为空");
            return result;
        }

        JobPost jobPost = jobPostService.getById(id);
        if (jobPost == null) {
            result.put("code", 404);
            result.put("msg", "岗位不存在");
            return result;
        }

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", jobPost);
        return result;
    }

    /**
     * HR 更新岗位
     */
    @PutMapping("/{id}")
    public Map<String, Object> updateJob(
            @PathVariable("id") Long id,
            @RequestBody JobPostUpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        Map<String, Object> result = new HashMap<>();

        if (userId == null) {
            result.put("code", 401);
            result.put("msg", "未检测到登录用户信息！");
            return result;
        }
        if (id == null) {
            result.put("code", 400);
            result.put("msg", "岗位 ID 不能为空");
            return result;
        }
        if (request == null || !StringUtils.hasText(request.getTitle())) {
            result.put("code", 400);
            result.put("msg", "岗位名称不能为空");
            return result;
        }

        JobPost existing = jobPostService.getById(id);
        if (existing == null) {
            result.put("code", 404);
            result.put("msg", "岗位不存在");
            return result;
        }

        existing.setTitle(request.getTitle().trim());
        if (StringUtils.hasText(request.getDeptName())) {
            existing.setDeptName(request.getDeptName().trim());
        }
        if (StringUtils.hasText(request.getWorkCity())) {
            existing.setWorkCity(request.getWorkCity().trim());
        }
        if (request.getEmploymentType() != null) {
            existing.setEmploymentType(request.getEmploymentType());
        }
        if (request.getHeadcount() != null) {
            existing.setHeadcount(request.getHeadcount());
        }
        if (request.getPriority() != null) {
            existing.setPriority(request.getPriority());
        }
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        if (request.getSalaryNegotiable() != null) {
            existing.setSalaryNegotiable(request.getSalaryNegotiable());
            if (Boolean.TRUE.equals(request.getSalaryNegotiable())) {
                existing.setSalaryMin(null);
                existing.setSalaryMax(null);
            }
        }
        if (!Boolean.TRUE.equals(existing.getSalaryNegotiable())) {
            if (request.getSalaryMin() != null) {
                existing.setSalaryMin(request.getSalaryMin());
            }
            if (request.getSalaryMax() != null) {
                existing.setSalaryMax(request.getSalaryMax());
            }
        }
        if (request.getJobDescription() != null) {
            existing.setJobDescription(request.getJobDescription());
        }
        if (request.getJobRequirements() != null) {
            existing.setJobRequirements(request.getJobRequirements());
        }
        if (request.getSkillTags() != null) {
            existing.setSkillTags(request.getSkillTags());
        }

        boolean success = jobPostService.updateById(existing);
        if (success) {
            result.put("code", 200);
            result.put("msg", "岗位更新成功");
            result.put("data", existing);
        } else {
            result.put("code", 500);
            result.put("msg", "岗位更新失败");
        }
        return result;
    }

    /**
     * HR 删除岗位（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteJob(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        Map<String, Object> result = new HashMap<>();

        if (userId == null) {
            result.put("code", 401);
            result.put("msg", "未检测到登录用户信息！");
            return result;
        }
        if (id == null) {
            result.put("code", 400);
            result.put("msg", "岗位 ID 不能为空");
            return result;
        }

        JobPost existing = jobPostService.getById(id);
        if (existing == null) {
            result.put("code", 404);
            result.put("msg", "岗位不存在");
            return result;
        }

        boolean success = jobPostService.removeById(id);
        if (success) {
            result.put("code", 200);
            result.put("msg", "岗位已删除");
        } else {
            result.put("code", 500);
            result.put("msg", "岗位删除失败");
        }
        return result;
    }
}