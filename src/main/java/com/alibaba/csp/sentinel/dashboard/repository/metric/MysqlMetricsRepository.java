/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.dashboard.repository.metric;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;
import com.alibaba.csp.sentinel.dashboard.mapperdao.SentinelMetrics;
import com.alibaba.csp.sentinel.dashboard.mapperdao.SentinelMetricsDao;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.csp.sentinel.util.TimeUtil;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Caches metrics data in a period of time in memory.
 *
 * @author Carpenter Lee
 * @author Eric Zhao
 */
@Component
public class MysqlMetricsRepository implements MetricsRepository<MetricEntity> {

    private static final long MAX_METRIC_LIVE_TIME_MS = 1000 * 60 * 5;

    /**
     * {@code app -> resource -> timestamp -> metric}
     */
    private Map<String, Map<String, LinkedHashMap<Long, MetricEntity>>> allMetrics = new ConcurrentHashMap<>();

    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();


    @Autowired
    private SentinelMetricsDao sentinelMetricsDao;
    @Override
    public void save(MetricEntity entity) {
        if (entity == null || StringUtil.isBlank(entity.getApp())) {
            return;
        }
        readWriteLock.writeLock().lock();
        try {
           /* allMetrics.computeIfAbsent(entity.getApp(), e -> new HashMap<>(16))
                    .computeIfAbsent(entity.getResource(), e -> new LinkedHashMap<Long, MetricEntity>() {
                        @Override
                        protected boolean removeEldestEntry(Entry<Long, MetricEntity> eldest) {
                            // Metric older than {@link #MAX_METRIC_LIVE_TIME_MS} will be removed.
                            return eldest.getKey() < TimeUtil.currentTimeMillis() - MAX_METRIC_LIVE_TIME_MS;
                        }
                    }).put(entity.getTimestamp().getTime(), entity);*/
            String jsonString = JSON.toJSONString(entity);
            SentinelMetrics sentinelMetrics = JSON.parseObject(jsonString, SentinelMetrics.class);
            sentinelMetricsDao.insert(sentinelMetrics);
        } finally {
            readWriteLock.writeLock().unlock();
        }

    }

    @Override
    public void saveAll(Iterable<MetricEntity> metrics) {
        if (metrics == null) {
            return;
        }
        readWriteLock.writeLock().lock();
        try {
            metrics.forEach(this::save);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public List<MetricEntity> queryByAppAndResourceBetween(String app, String resource,
                                                           long startTime, long endTime) {
        List<MetricEntity> results = new ArrayList<>();
        if (StringUtil.isBlank(app)) {
            return results;
        }
        Date date = new Date(startTime);
        Date end = new Date(endTime);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String string = sdf.format(date);
        String enstring = sdf.format(end);
        Example example = new Example(SentinelMetrics.class);
        example.and().andEqualTo("app",app).andEqualTo("resource",resource).andLessThanOrEqualTo("timestamp",enstring).andGreaterThanOrEqualTo("timestamp",string);
        example.orderBy("timestamp").desc();
        List<SentinelMetrics> sentinelMetrics = sentinelMetricsDao.selectByExample(example);
        if (CollectionUtils.isEmpty(sentinelMetrics)) {
            return  new ArrayList<>();
        }
        List<MetricEntity> collect = sentinelMetrics.stream().map(r -> {
            MetricEntity metricEntity = new MetricEntity();
            BeanUtils.copyProperties(r, metricEntity);
            return metricEntity;
        }).collect(Collectors.toList());
        return collect;
      /*  Map<String, LinkedHashMap<Long, MetricEntity>> resourceMap = allMetrics.get(app);
        if (resourceMap == null) {
            return results;
        }
        LinkedHashMap<Long, MetricEntity> metricsMap = resourceMap.get(resource);
        if (metricsMap == null) {
            return results;
        }
        readWriteLock.readLock().lock();
        try {
            for (Entry<Long, MetricEntity> entry : metricsMap.entrySet()) {
                if (entry.getKey() >= startTime && entry.getKey() <= endTime) {
                    results.add(entry.getValue());
                }
            }
            return results;
        } finally {
            readWriteLock.readLock().unlock();
        }*/
    }

    @Override
    public List<String> listResourcesOfApp(String app) {
        List<String> results = new ArrayList<>();
        if (StringUtil.isBlank(app)) {
            return results;
        }
        // resource -> timestamp -> metric
        Example example = new Example(SentinelMetrics.class);
        example.and().andEqualTo("app",app);
        example.orderBy("timestamp").desc();
        List<SentinelMetrics> sentinelMetrics = sentinelMetricsDao.selectByExample(example);
        if (CollectionUtils.isEmpty(sentinelMetrics)) {
            return  new ArrayList<>();
        }
        List<MetricEntity> metricsList = sentinelMetrics.stream().map(r -> {
            MetricEntity metricEntity = new MetricEntity();
            BeanUtils.copyProperties(r, metricEntity);
            return metricEntity;
        }).toList();
        Map<String, LinkedHashMap<Long, MetricEntity>> result = metricsList.stream()
                .collect(Collectors.groupingBy(
                        MetricEntity::getResource,  // 外层Map的key
                        LinkedHashMap::new,     // 外层Map使用LinkedHashMap
                        Collectors.toMap(
                                r -> r.getTimestamp().getTime(),  // 内层Map的key
                                Function.identity(),         // 内层Map的value
                                (oldValue, newValue) -> oldValue,  // 合并函数(处理key冲突)
                                LinkedHashMap::new           // 内层Map使用LinkedHashMap
                        )
                ));
        // Map<String, LinkedHashMap<Long, MetricEntity>> resourceMap = allMetrics.get(app);
        Map<String, LinkedHashMap<Long, MetricEntity>> resourceMap = result;
        if (resourceMap == null) {
            return results;
        }
        final long minTimeMs = System.currentTimeMillis() - 1000 * 60;
        Map<String, MetricEntity> resourceCount = new ConcurrentHashMap<>(32);

        readWriteLock.readLock().lock();
        try {
            for (Entry<String, LinkedHashMap<Long, MetricEntity>> resourceMetrics : resourceMap.entrySet()) {
                for (Entry<Long, MetricEntity> metrics : resourceMetrics.getValue().entrySet()) {
                    // if (metrics.getKey() < minTimeMs) {
                    //     continue;
                    // }
                    MetricEntity newEntity = metrics.getValue();
                    if (resourceCount.containsKey(resourceMetrics.getKey())) {
                        MetricEntity oldEntity = resourceCount.get(resourceMetrics.getKey());
                        oldEntity.addPassQps(newEntity.getPassQps());
                        oldEntity.addRtAndSuccessQps(newEntity.getRt(), newEntity.getSuccessQps());
                        oldEntity.addBlockQps(newEntity.getBlockQps());
                        oldEntity.addExceptionQps(newEntity.getExceptionQps());
                        oldEntity.addCount(1);
                    } else {
                        resourceCount.put(resourceMetrics.getKey(), MetricEntity.copyOf(newEntity));
                    }
                }
            }
            // Order by last minute b_qps DESC.
            return resourceCount.entrySet()
                    .stream()
                    .sorted((o1, o2) -> {
                        MetricEntity e1 = o1.getValue();
                        MetricEntity e2 = o2.getValue();
                        int t = e2.getBlockQps().compareTo(e1.getBlockQps());
                        if (t != 0) {
                            return t;
                        }
                        return e2.getPassQps().compareTo(e1.getPassQps());
                    })
                    .map(Entry::getKey)
                    .collect(Collectors.toList());
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    @Override
    public List<String> listResourcesOfApp(String app, Integer pageSize, Integer curPage) {
        List<String> results = new ArrayList<>();
        if (StringUtil.isBlank(app)) {
            return results;
        }
        // resource -> timestamp -> metric
        Example example = new Example(SentinelMetrics.class);
        example.and().andEqualTo("app",app);
        example.orderBy("timestamp").desc();
        PageHelper.startPage(curPage, pageSize);
        List<SentinelMetrics> sentinelMetrics = sentinelMetricsDao.selectByExample(example);
        if (CollectionUtils.isEmpty(sentinelMetrics)) {
            return  new ArrayList<>();
        }
        List<MetricEntity> metricsList = sentinelMetrics.stream().map(r -> {
            MetricEntity metricEntity = new MetricEntity();
            BeanUtils.copyProperties(r, metricEntity);
            return metricEntity;
        }).toList();
        Map<String, LinkedHashMap<Long, MetricEntity>> result = metricsList.stream()
                .collect(Collectors.groupingBy(
                        MetricEntity::getResource,  // 外层Map的key
                        LinkedHashMap::new,     // 外层Map使用LinkedHashMap
                        Collectors.toMap(
                                r -> r.getTimestamp().getTime(),  // 内层Map的key
                                Function.identity(),         // 内层Map的value
                                (oldValue, newValue) -> oldValue,  // 合并函数(处理key冲突)
                                LinkedHashMap::new           // 内层Map使用LinkedHashMap
                        )
                ));
        // Map<String, LinkedHashMap<Long, MetricEntity>> resourceMap = allMetrics.get(app);
        Map<String, LinkedHashMap<Long, MetricEntity>> resourceMap = result;
        if (resourceMap == null) {
            return results;
        }
        final long minTimeMs = System.currentTimeMillis() - 1000 * 60;
        Map<String, MetricEntity> resourceCount = new ConcurrentHashMap<>(32);

            for (Entry<String, LinkedHashMap<Long, MetricEntity>> resourceMetrics : resourceMap.entrySet()) {
                for (Entry<Long, MetricEntity> metrics : resourceMetrics.getValue().entrySet()) {
                    // if (metrics.getKey() < minTimeMs) {
                    //     continue;
                    // }
                    MetricEntity newEntity = metrics.getValue();
                    if (resourceCount.containsKey(resourceMetrics.getKey())) {
                        MetricEntity oldEntity = resourceCount.get(resourceMetrics.getKey());
                        oldEntity.addPassQps(newEntity.getPassQps());
                        oldEntity.addRtAndSuccessQps(newEntity.getRt(), newEntity.getSuccessQps());
                        oldEntity.addBlockQps(newEntity.getBlockQps());
                        oldEntity.addExceptionQps(newEntity.getExceptionQps());
                        oldEntity.addCount(1);
                    } else {
                        resourceCount.put(resourceMetrics.getKey(), MetricEntity.copyOf(newEntity));
                    }
                }
            }
            // Order by last minute b_qps DESC.
            return resourceCount.entrySet()
                    .stream()
                    .sorted((o1, o2) -> {
                        MetricEntity e1 = o1.getValue();
                        MetricEntity e2 = o2.getValue();
                        int t = e2.getBlockQps().compareTo(e1.getBlockQps());
                        if (t != 0) {
                            return t;
                        }
                        return e2.getPassQps().compareTo(e1.getPassQps());
                    })
                    .map(Entry::getKey)
                    .collect(Collectors.toList());
    }
}
