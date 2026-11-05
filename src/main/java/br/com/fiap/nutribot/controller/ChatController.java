package br.com.fiap.nutribot.controller;

import br.com.fiap.nutribot.dto.ChatRequest;
import br.com.fiap.nutribot.dto.ChatResponse;
import br.com.fiap.nutribot.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Chat com a NutriBot
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService service;

    @PostMapping
    public ChatResponse chat(@RequestBody @Valid ChatRequest request) {
        return new ChatResponse(service.chat(request.conversationId(), request.message()));
    }
}
