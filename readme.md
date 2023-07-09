## Notes

1. Code debugging using ecllipse is detailed in 人狼知能マニュアル_20230127.pptx (in google drive).
2. Jar file should not contain aiwolf-client.jar, aiwolf-server.jar, aiwolf-common.jar, aiwolf-viewer.jar, jsonic-xxxx.jar, as written in the [regulation](https://github.com/aiwolf/CompetitionProtocolDivision/blob/main/en/regulation.md) "Please be aware that there may be conflicts if you include the following files in your jar file".
3. Agent is intialized everytime a game starts while the program is loaded only once per game set.
4. Logging can be turned off by changing logger.setLevel(Level.FINE) to Level.INFO.
5. Unnecessary print out can be cleaned by cleanning up println in the legacy code.
6. Code changed are annotated by "// yutian". Please use the search facility and git diff tool to speed up code merging. Note the import part of BasketBasePlayer is totally changed.
7. To get prediction result, use predict function defined in the base player. Please do not modify or create input parameters, i.e., sm, env, session, logger. Instead, directly use player's existing ones, e.g., player.sm.
8. One missuse I found is the getLatestVoteList function and getVoteList function. The former returns a non-empty list only when revote happens while the latter only returns non-empty list after the day changes.
9. Please put CNNLSTM_xxx.onnx file (in google drive) at the root directory, i.e., the parent directory of src.
10. New file ONNX.java is not used (only for personal debugging), while StatusMatrix.java is used.