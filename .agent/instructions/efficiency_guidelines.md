# Agent Token Efficiency Guidelines

To minimize token usage and optimize performance, all agents MUST follow these instructions:

## 1. Code Exploration
- **Targeted Search**: Use `grep_search` or `find_by_name` to locate specific code patterns or files rather than listing large directories.
- **Read Sparingly**: When using `view_file`, specify `StartLine` and `EndLine` to read only the necessary chunks. Only read the full file if absolutely necessary.

## 2. Code Modification
- **Batch Edits**: Use `multi_replace_file_content` to make multiple changes to a single file in one tool call.
- **Precision**: Ensure `TargetContent` is unique and precise to avoid multiple round-trips for fixing "multiple occurrences found" errors.
- **Avoid Over-Correction**: Focus on the specific task. Don't refactor unrelated code unless it's critical for the task or explicitly requested.

## 3. Terminal Usage
- **Limit Output**: When running commands, use flags to limit output (e.g., `git log -n 5`, `head`, `tail`).
- **Skip Unnecessary Steps**: Skip tests (`-DskipTests`) during intermediate builds if you only need to verify compilation or run a specific service.
- **Monitor Status**: Use `command_status` with appropriate `WaitDurationSeconds` to avoid excessive polling.

## 4. Problem Solving
- **Plan First**: Think through the entire solution before calling tools.
- **Single Source of Truth**: Don't ask the user for information you can find in the codebase or environment.
- **Context Awareness**: Use the provided `ADDITIONAL_METADATA` (open files, cursor position) to skip redundant exploration.

## 5. Response Optimization
- **Be Concise**: Keep explanations brief and technical. Use markdown for clarity.
- **Summarize Actions**: Instead of listing every minute detail, summarize the outcome of your actions.
