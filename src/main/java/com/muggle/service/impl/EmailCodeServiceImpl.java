package com.muggle.service.impl;

import com.muggle.component.RedisComponent;
import com.muggle.entity.config.AppConfig;
import com.muggle.entity.constants.Constants;
import com.muggle.entity.dto.SysSettingsDto;
import com.muggle.entity.po.EmailCode;
import com.muggle.entity.po.UserInfo;
import com.muggle.entity.query.EmailCodeQuery;
import com.muggle.entity.query.UserInfoQuery;
import com.muggle.exception.BusinessException;
import com.muggle.mappers.EmailCodeMapper;
import com.muggle.mappers.UserInfoMapper;
import com.muggle.service.EmailCodeService;
import com.muggle.utils.StringTools;
import java.util.Date;
import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service("emailCodeService")
public class EmailCodeServiceImpl implements EmailCodeService {
  private static final Logger logger = LoggerFactory.getLogger(EmailCodeServiceImpl.class);
  @Resource JavaMailSender javaMailSender;
  @Resource AppConfig appConfig;
  @Resource private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;
  @Resource private EmailCodeMapper<EmailCode, EmailCodeQuery> emailCodeMapper;
  @Resource private RedisComponent redisComponent;

  /**
   * 发送邮件验证码
   *
   * @param toEmail 邮箱
   * @param code 验证码
   */
  private void sendEmailCode(String toEmail, String code) {
    try {
      MimeMessage mimeMailMessage = javaMailSender.createMimeMessage();
      MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMailMessage);

      // 从 redis 获取系统设置
      SysSettingsDto sysSettingsDto = redisComponent.getSysSettings();

      // 邮件的发件人
      mimeMessageHelper.setFrom(appConfig.getSendUserName());
      // 邮件的收件人
      mimeMessageHelper.setTo(toEmail);
      mimeMessageHelper.setSentDate(new Date());
      // 邮件标题和内容
      mimeMessageHelper.setSubject(sysSettingsDto.getRegisterEmailTitle());
      mimeMessageHelper.setText(
          String.format(sysSettingsDto.getRegisterEmailContent(), code), true);
      mimeMessageHelper.setSentDate(new Date());
      // 发送邮件
      javaMailSender.send(mimeMailMessage);

    } catch (MessagingException e) {
      logger.error("邮件发送失败", e);
      throw new BusinessException("邮件发送失败");
    }
  }

  @Override
  public void sendEmailCode(String email, Integer type) {
    // 如果是用户注册，校验邮箱是否存在
    if (type == Constants.ZERO) {
      UserInfo userInfo = userInfoMapper.selectByEmail(email);
      // 如果邮箱已存在，抛出异常
      if (userInfo != null) {
        throw new RuntimeException("邮箱已存在");
      }

      // 每次发送邮件前都要禁用之前的验证码
      emailCodeMapper.disableEmailCode(email);

      // 验证码
      String code = StringTools.getRandomNumber(Constants.LENGTH_5);
      // 发送验证码
      sendEmailCode(email, code);

      // 保存验证码
      EmailCode emailCode = new EmailCode();
      emailCode.setCode(code);
      emailCode.setEmail(email);
      emailCode.setStatus(Constants.ZERO);
      emailCode.setCreateTime(new Date());
      emailCodeMapper.insert(emailCode);
    }
  }

  /**
   * 校验邮箱验证码
   * @param email
   * @param code
   */
  @Override
  public void checkEmailCode(String email, String code) {
    EmailCode emailCode = this.emailCodeMapper.selectByEmailAndCode(email, code);

    if (emailCode == null) {
      throw new BusinessException("邮箱验证码错误");
    }

    if (emailCode.getStatus() == 1
        || System.currentTimeMillis() - emailCode.getCreateTime().getTime()
            > Constants.LENGTH_15 * 1000 * 60) {
      throw new BusinessException("邮箱验证码已失效");
    }

    emailCodeMapper.disableEmailCode(email);
  }
}
