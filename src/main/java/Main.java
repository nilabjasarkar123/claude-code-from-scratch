
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;

import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 2 || !"-p".equals(args[0])) {
            System.err.println("Usage: program -p <prompt>");
            System.exit(1);
        }

        String prompt = args[1];

        String apiKey = System.getenv("OPENROUTER_API_KEY");
        String baseUrl = System.getenv("OPENROUTER_BASE_URL");

        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "https://openrouter.ai/api/v1";
        }

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("OPENROUTER_API_KEY is not set");
        }

        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();

        ChatCompletionTool readTool = ChatCompletionTool.builder()
                .function(
                        FunctionDefinition.builder()
                                .name("Read")
                                .description(
                                        "Read and return the contents of a file")
                                .parameters(
                                        JsonValue.from(Map.of(
                                                "type", "object",
                                                "properties", Map.of(
                                                        "file_path", Map.of(
                                                                "type", "string",
                                                                "description",
                                                                "The path to the file to read"
                                                        )
                                                ),
                                                "required", List.of("file_path")
                                        ))
                                )
                                .build()
                )
                .build();

        ChatCompletionCreateParams.Builder params =
                ChatCompletionCreateParams.builder()
                        .model("anthropic/claude-haiku-4.5")
                        .addUserMessage(prompt)
                        .addTool(readTool);

        ChatCompletion response =
                client.chat().completions().create(params.build());

        if (response.choices().isEmpty()) {
            throw new RuntimeException("No choices in response");
        }

        var message = response.choices().get(0).message();

        // TODO: Inspect the assistant message's tool calls.
        // For each Read call:
        // 1. Parse its JSON arguments into ReadFileTool.
        var toolCalls = message.toolCalls().orElse(List.of());

        if (toolCalls.isEmpty()) {
            System.out.print(message.content().orElse(""));
            return;
        }

        var toolCall = toolCalls.get(0);

        String argumentsJson = toolCall.function().arguments();

        ObjectMapper objectMapper = new ObjectMapper();

        ReadFileTool readFileTool = objectMapper.readValue(
                argumentsJson,
                ReadFileTool.class
        );

        String contents = readFileTool.execute();

        // 2. Call execute() to read the file.
        // 3. Append the assistant message and tool result to params.
        // 4. Call the model again with params.build().
        // 5. Print the final response, not the initial tool-call message.

        // Preserve the assistant message containing the tool call.
        params.addMessage(message);

        // Return the file contents as the tool result.
        params.addMessage(
                ChatCompletionToolMessageParam.builder()
                        .toolCallId(toolCall.id())
                        .content(contents)
                        .build()
        );

        // Ask the model to respond using the file contents.
        ChatCompletion finalResponse =
                client.chat().completions().create(params.build());

        if (finalResponse.choices().isEmpty()) {
            throw new RuntimeException("No choices in final response");
        }

        System.out.print(
                finalResponse.choices().get(0)
                        .message()
                        .content()
                        .orElse("")
        );
    }
}
