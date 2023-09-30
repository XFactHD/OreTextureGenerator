package xfacthd.oretexgen.client.util;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.OptionalFieldCodec;

import java.util.Optional;

public final class BetterOptionalFieldCodec<A> extends OptionalFieldCodec<A>
{
    private final String name;
    private final Codec<A> elementCodec;

    public BetterOptionalFieldCodec(String name, Codec<A> elementCodec)
    {
        super(name, elementCodec);
        this.name = name;
        this.elementCodec = elementCodec;
    }

    @Override
    public <T> DataResult<Optional<A>> decode(DynamicOps<T> ops, MapLike<T> input)
    {
        final T value = input.get(name);
        if (value == null)
        {
            return DataResult.success(Optional.empty());
        }

        final DataResult<A> parsed = elementCodec.parse(ops, value);
        if (parsed.result().isPresent() || parsed.error().isPresent())
        {
            return parsed.map(Optional::of);
        }
        return DataResult.success(Optional.empty());
    }
}
