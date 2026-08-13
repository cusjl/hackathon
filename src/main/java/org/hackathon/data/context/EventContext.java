package org.hackathon.data.context;

import lombok.Data;
import org.hackathon.data.po.Event;
import org.hackathon.data.po.Phase;
import org.hackathon.data.po.Track;

@Data
public class EventContext {
    private Event event;
    private Track track;
    private Phase phase;
}
