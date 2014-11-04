package signalJ.models;

import play.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public class CallerState  {
    public final Map<String, String> _values;
    public final Map<String, String> _oldValues = new HashMap<>();

    public CallerState() {
        _values = new HashMap<>();
    }

    public CallerState(Map<String, String> _values) {
        this._values = _values;
    }

    public void put(String key, String value) {
        if (!_oldValues.containsKey(key)) _oldValues.put(key, _values.get(key));
        _values.put(key, value);
    }

    public String get(String key) {
        return _values.get(key);
    }

    public Optional<Map<String, String>> getChanges() {
        Map<String, String> changes = new HashMap<>();
        _oldValues.keySet().forEach(k -> changes.put(k, _values.get(k)));
        return changes.isEmpty() ? Optional.empty() : Optional.of(changes);
    }

    public void forEach(BiConsumer<? super String,? super String> action) {
        for (Map.Entry<String, String> entry : _values.entrySet())
            action.accept(entry.getKey(), entry.getValue());
    }
}