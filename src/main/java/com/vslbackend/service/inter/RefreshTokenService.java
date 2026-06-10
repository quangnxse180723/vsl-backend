package com.vslbackend.service.inter;

import com.vslbackend.entity.RefreshToken;
import com.vslbackend.entity.User;

public interface RefreshTokenService {

    /** Tao moi refresh token cho user (thay the token cu neu co - xoay vong token). */
    RefreshToken createRefreshToken(User user);

    /** Tim token, nem loi neu khong ton tai hoac da het han. */
    RefreshToken validateAndGet(String token);

    /** Thu hoi refresh token (dung khi logout). */
    void deleteByUser(User user);
}
