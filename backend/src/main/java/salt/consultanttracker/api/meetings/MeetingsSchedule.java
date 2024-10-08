package salt.consultanttracker.api.meetings;


import salt.consultanttracker.api.responsiblept.ResponsiblePT;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MeetingsSchedule {

    @EmbeddedId
    private MeetingsScheduleKey id;

    private LocalDate date;
    private String status;

    @ManyToOne
    @JoinColumn(name = "responsiblePT_id", referencedColumnName = "id")
    private ResponsiblePT responsiblePT;

}
