package com.verifipay.auth.repository;
import com.verifipay.auth.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface RefreshTokenRepository extends JpaRepository<RefreshToken,Long>{
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from RefreshToken r join fetch r.user where r.tokenHash=:hash") Optional<RefreshToken> findByTokenHashForUpdate(@Param("hash") String hash);
}
