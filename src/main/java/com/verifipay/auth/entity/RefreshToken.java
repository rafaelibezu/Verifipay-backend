package com.verifipay.auth.entity;
import com.verifipay.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
@Entity @Table(name="refresh_tokens") @Getter @NoArgsConstructor
public class RefreshToken {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id") private User user;
 @Column(name="token_hash", nullable=false, unique=true, length=64) private String tokenHash;
 @Column(name="expires_at", nullable=false) private Instant expiresAt;
 @Column(name="revoked_at") private Instant revokedAt;
 @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="replaced_by_token_id") private RefreshToken replacedByToken;
 @Column(name="created_at", nullable=false, updatable=false) private Instant createdAt;
 public RefreshToken(User user,String hash,Instant expiresAt){this.user=user;this.tokenHash=hash;this.expiresAt=expiresAt;}
 public void revoke(Instant now){if(revokedAt==null) revokedAt=now;}
 public void replaceBy(RefreshToken token){replacedByToken=token;}
 @PrePersist void create(){createdAt=Instant.now();}
}
