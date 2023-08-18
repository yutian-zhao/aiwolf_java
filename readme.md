## AIWolf java
This is the repository for aiwolf agent Howls that attends the [5th AIWolf competition](http://aiwolf.org/en/archives/2901). The code is developed based on the championship agent [Basket](http://aiwolf.org/archives/2840) (in japanese) in the 4th international AIWolf competition. The main difference is replace the original role prediction mechanism, which uses Bayesian inference based on the past game statistics, with neural network predictions. In the master branch, to keep changes in a comnon place, only ```Base Player``` class is modified and ```Status Matrix``` class is added to support neural network prediction. While in the ```policy_only``` branch, the overall policy are modifiied execulding the model. 

## Notes
1. On the master branch, code changed are annotated by "// yutian". Please use the search facility and git diff tool to speed up code merging. Note the import part of BasketBasePlayer is totally changed.
2. To get prediction result, use predict function defined in the base player. Please do not modify or create input parameters, i.e., sm, env, session, logger. Instead, directly use player's existing ones, e.g., player.sm.
3. One missuse I found is the getLatestVoteList function and getVoteList function. The former returns a non-empty list only when revote happens while the latter only returns non-empty list after the day changes.
4. Please put CNNLSTM_xxx.onnx file in the same directory as code files.
5. The project depends on the [onnx runtime 1.15.1](https://jar-download.com/artifact-search/onnxruntime). Please download and include the jar file in the class path, and refer to the [official document](https://onnxruntime.ai/docs/get-started/with-java.html#api-reference) for more information.
