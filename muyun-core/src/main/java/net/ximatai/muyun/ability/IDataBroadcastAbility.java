package net.ximatai.muyun.ability;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import net.ximatai.muyun.model.DataChangeChannel;

public interface IDataBroadcastAbility extends IMetadataAbility {

    EventBus getEventBus();

    default DataChangeChannel getDataChangeChannel() {
        return new DataChangeChannel(this);
    }

    default boolean msgToFrontEnd() {
        return true;
    }

    default void broadcast(DataChangeChannel.Type type, String id) {
        EventBus eventBus = getEventBus();
        DataChangeChannel channel = getDataChangeChannel();
        String address = channel.getAddress();
        String addressWithType = channel.getAddressWithType(type);

        JsonObject body = new JsonObject();
        body.put("type", type.name());
        body.put("id", id);
        if (msgToFrontEnd()) {
            body.put("toFrontEnd", true);
        }

        eventBus.publish(address, body);
        eventBus.publish(addressWithType, body);
    }
}