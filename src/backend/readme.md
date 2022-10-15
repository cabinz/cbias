# Structure

The backend receive the IR and translate into assemble file.

Pipeline: IR => MCBuilder => Optimize => MCEmitter => Assembly

![pipeline of backend](../../doc/image/pipeline_of_backend.jpg)