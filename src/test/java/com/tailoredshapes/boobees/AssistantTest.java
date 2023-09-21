package com.tailoredshapes.boobees;

import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.*;
import com.theokanning.openai.service.OpenAiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AssistantTest {

    private OpenAiService openAIClient;
    private MessageRepo messageRepo;
    private Assistant assistant;

    @BeforeEach
    void setUp() {
        openAIClient = Mockito.mock(OpenAiService.class);
        messageRepo = Mockito.mock(MessageRepo.class);

        String failMessage = "Sorry, I couldn't understand that.";
        assistant = new Assistant(openAIClient, failMessage, messageRepo);
    }

    @Test
    void answerShouldReturnValidResponse() {
        List<String> prompts = Arrays.asList("What's your name?", "Tell me a joke.");
        Long chatId = 42L;

        ChatCompletionResult chatCompletions = Mockito.mock(ChatCompletionResult.class);

        when(openAIClient.createChatCompletion(any())).thenReturn(chatCompletions);
        ChatCompletionChoice choice = new ChatCompletionChoice();
        choice.setMessage(new ChatMessage(ChatMessageRole.ASSISTANT.value(), "I'm GPT-3.5-turbo, and here's a joke: Why did the chicken cross the road? To get to the other side!"));

        when(chatCompletions.getChoices()).thenReturn(Arrays.asList(choice));
        when(messageRepo.findLastN(any(Long.class), any(Integer.class))).thenReturn(Collections.emptyList());

        String response = assistant.answer(prompts, chatId);

        assertEquals("I'm GPT-3.5-turbo, and here's a joke: Why did the chicken cross the road? To get to the other side!", response);
    }

    @Test
    void answerAsyncShouldReturnValidResponse() throws Exception{
        List<String> prompts = Arrays.asList("What's your name?", "Tell me a joke.");
        Long chatId = 42L;

        ChatCompletionResult chatCompletions = Mockito.mock(ChatCompletionResult.class);

        when(openAIClient.createChatCompletion(any())).thenReturn(chatCompletions);
        ChatCompletionChoice choice = new ChatCompletionChoice();
        choice.setMessage(new ChatMessage(ChatMessageRole.ASSISTANT.value(), "I'm GPT-3.5-turbo, and here's a joke: Why did the chicken cross the road? To get to the other side!"));

        when(chatCompletions.getChoices()).thenReturn(Arrays.asList(choice));
        when(messageRepo.findLastN(any(Long.class), any(Integer.class))).thenReturn(Collections.emptyList());


        String response = assistant.answerAsync(prompts, chatId).get();

        assertEquals("I'm GPT-3.5-turbo, and here's a joke: Why did the chicken cross the road? To get to the other side!", response);
    }
}
