package com.giabao.linkflow.auth.session;

import java.util.UUID;

public record RefreshSession(UUID userId, String refreshTokenHash) {
}
