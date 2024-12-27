package com.muggle.component;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component("redisUtils")
public class RedisUtils<V> {

  private static final Logger logger = LoggerFactory.getLogger(RedisUtils.class);

  @Resource private RedisTemplate<String, V> redisTemplate;

  /**
   * 删除一个或多个key
   *
   * @param keys
   */
  public void delete(String... keys) {
    if (keys != null && keys.length > 0) {
      if (keys.length == 1) {
        redisTemplate.delete(keys[0]);
      } else {
        redisTemplate.delete((Collection<String>) CollectionUtils.arrayToList(keys));
      }
    }
  }

  /**
   * 根据 key 获取 value
   *
   * @param key
   * @return
   */
  public V get(String key) {
    return key == null ? null : redisTemplate.opsForValue().get(key);
  }

  /**
   * 设置 key value
   *
   * @param key
   * @param val
   * @return
   */
  public boolean set(String key, V val) {
    try {
      redisTemplate.opsForValue().set(key, val);
      logger.error("设置 redis key: {}, value: {}", key, val);
      return true;
    } catch (Exception e) {
      logger.error("设置 redis key: {}, value: {} 失败", key, val);
      return false;
    }
  }

  /**
   * 设置 key value 并设置过期时间
   *
   * @param key
   * @param val
   * @param time
   * @return
   */
  public boolean setex(String key, V val, long time) {
    try {
      if (time > 0) {
        redisTemplate.opsForValue().set(key, val, time, TimeUnit.SECONDS);
      } else {
        set(key, val);
      }
      return true;
    } catch (Exception e) {
      logger.error("设置 redis key: {}, value: {} 失败", key, val);
      return false;
    }
  }
}
