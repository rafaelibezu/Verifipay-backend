package com.verifipay.auth.service;
import com.verifipay.auth.entity.RefreshToken;
import com.verifipay.auth.repository.RefreshTokenRepository;
import com.verifipay.common.exception.BusinessException;
import com.verifipay.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets; import java.security.*; import java.time.Instant; import java.time.temporal.ChronoUnit; import java.util.Base64;
@Service @RequiredArgsConstructor public class RefreshTokenService {
 private final RefreshTokenRepository tokens; private final SecureRandom random=new SecureRandom();
 @Value("${security.jwt.refresh-expiration-days:30}") private long days;
 public record Issued(String value, RefreshToken token) {}
 public Issued issue(User user){byte[] bytes=new byte[32];random.nextBytes(bytes);String value=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);return new Issued(value,tokens.save(new RefreshToken(user,hash(value),Instant.now().plus(days,ChronoUnit.DAYS))));}
 @Transactional public Issued rotate(String value){RefreshToken old=find(value);Instant now=Instant.now(); if(old.getRevokedAt()!=null||!old.getExpiresAt().isAfter(now)||!Boolean.TRUE.equals(old.getUser().getActive())) throw invalid(); old.revoke(now); Issued next=issue(old.getUser()); old.replaceBy(next.token()); return next;}
 @Transactional public void logout(String value){if(value==null||value.isBlank()) return; tokens.findByTokenHashForUpdate(hash(value)).ifPresent(t->t.revoke(Instant.now()));}
 private RefreshToken find(String value){if(value==null||value.isBlank())throw invalid();return tokens.findByTokenHashForUpdate(hash(value)).orElseThrow(this::invalid);}
 private BusinessException invalid(){return new BusinessException("Refresh token invalido o expirado");}
 private String hash(String value){try{return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
