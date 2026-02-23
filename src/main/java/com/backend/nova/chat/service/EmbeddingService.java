package com.backend.nova.chat.service;

import java.util.List;

public interface EmbeddingService {
    List<Float> embed(String text);
}
