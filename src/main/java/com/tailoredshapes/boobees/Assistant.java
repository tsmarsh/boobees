package com.tailoredshapes.boobees;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatMessage;
import com.azure.ai.openai.models.ChatRole;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.tailoredshapes.underbar.ocho.UnderBar.map;

public class Assistant {
    static {
        AWSXRayRecorderBuilder builder = AWSXRayRecorderBuilder.standard();
        AWSXRay.setGlobalRecorder(builder.build());
    }

    private static final Logger LOG = LogManager.getLogger(Assistant.class);
    private final OpenAIClient openAIClient;

    public final String failMessage;
    private final MessageRepo repo;

    public Assistant(OpenAIClient openAIClient, String failMessage, MessageRepo repo) {
        this.repo = repo;
        this.openAIClient = openAIClient;

        this.failMessage = failMessage;
    }

    public String answer(List<String> prompts, Long chatId) {

        var systemPrompt = new ChatMessage(ChatRole.SYSTEM).setContent(System.getenv("SYSTEM_PROMPT"));
        var formatPrompt = new ChatMessage(ChatRole.SYSTEM).setContent("Please use markdown for formatting and emphasis, feel free to use emoji.");
        LOG.info("Using personality: %s".formatted(systemPrompt));

        List<ChatMessage> lastN = repo.findLastN(chatId, 30);

        Collections.reverse(lastN);

        LOG.info("Found %d items for context".formatted(lastN.size()));

        List<ChatMessage> chatPrompts = map(prompts, (m) -> new ChatMessage(ChatRole.USER).setContent(m));

        List<ChatMessage> aiPrompts = new ArrayList<>(lastN);
        aiPrompts.add(formatPrompt);
        aiPrompts.add(systemPrompt);
        aiPrompts.addAll(chatPrompts);

        try {
            LOG.debug("Prompts sent to AI: \n" + new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(aiPrompts));
        } catch (JsonProcessingException e) {
            LOG.error("Can't display prompts", e);
        }


        ChatCompletionsOptions chatCompletionsOptions = new ChatCompletionsOptions(aiPrompts);
    //        chatCompletionsOptions.setMaxTokens(200);

        String message = failMessage;

        try(var ss = AWSXRay.beginSubsegment("Calling OpenAI")){
            try {
                ChatCompletions chatCompletions = openAIClient.getChatCompletions("gpt-3.5-turbo", chatCompletionsOptions);
                ChatMessage answer = chatCompletions.getChoices().get(0).getMessage();
                message = answer.getContent();

                chatPrompts.add(answer);
                repo.createAll(chatId, chatPrompts);
            } catch (Exception e) {
                LOG.error("OpenAI is screwing around again", e);
                ss.addException(e);
            }

        }

        return message;
    }

    public String quickAnswer(String prompt) {
        List<ChatMessage> aiPrompt = new ArrayList<>();
        aiPrompt.add(new ChatMessage(ChatRole.USER).setContent(prompt));

        ChatCompletionsOptions chatCompletionsOptions = new ChatCompletionsOptions(aiPrompt);

        String message = failMessage;

        try(var ss = AWSXRay.beginSubsegment("Calling OpenAI for quick answer")){
            try {
                ChatCompletions chatCompletions = openAIClient.getChatCompletions("gpt-3.5-turbo", chatCompletionsOptions);
                ChatMessage answer = chatCompletions.getChoices().get(0).getMessage();
                message = answer.getContent();
            } catch (Exception e) {
                LOG.error("OpenAI is screwing around again", e);
                ss.addException(e);
            }
        }

        return message;
    }

    public CompletableFuture<String> answerAsync(List<String> prompts, Long chatId) {
        return CompletableFuture.supplyAsync(() -> answer(prompts, chatId));
    }
}
