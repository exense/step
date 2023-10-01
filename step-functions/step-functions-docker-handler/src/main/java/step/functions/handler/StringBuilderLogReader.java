package step.functions.handler;

import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.model.Frame;

public class StringBuilderLogReader extends ResultCallbackTemplate.Adapter<Frame> {
    public StringBuilder builder;

    public StringBuilderLogReader(StringBuilder builder) {
        this.builder = builder;
    }

    @Override
    public void onNext(Frame item) {
        builder.append(new String(item.getPayload()));
        super.onNext(item);
    }
}
