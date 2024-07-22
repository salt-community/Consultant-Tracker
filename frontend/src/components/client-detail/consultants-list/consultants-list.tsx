import { useClientsContext } from "@/context/clients";
import SingleConsultant from "@/components/client-detail/consultants-list/single-consultant/single-consultant";
import { usePathname } from "next/navigation";
import Timeline from "react-calendar-timeline";
import "react-calendar-timeline/lib/Timeline.css";
import moment from "moment";
import { consultantsCalendar, consultantItems } from "@/mockData";
import { useEffect, useState } from "react";
import { ConsultantItemsType } from "@/types";
import { getDatesForRemainingTime } from "@/helperMethods";
import "./consultant-list.css";
import dayjs from "dayjs";
import { grey } from "@mui/material/colors";

const ConsultantsList = () => {
  const client = useClientsContext();
  const idParam = usePathname().split("/").pop();
  const singleClient = client.data.filter((el) => el.id === idParam)[0];
  const [items, setItems] = useState<ConsultantItemsType[]>(consultantItems);

  useEffect(() => {
    const remainingTimeItem = getDatesForRemainingTime(1, items);
    let tempItems = items;
    const newItem = {
      id: 10,
      group: 1,
      start_time: remainingTimeItem.start_date,
      end_time: remainingTimeItem.end_date,
      itemProps: {
        style: {
          zIndex: 1,
          backgroundColor: "grey",
        },
      },
    };
    tempItems.push(newItem);
    setItems(tempItems);
  }, []);

  console.log("All items", items);

  return (
    <div>
      <div>
        <Timeline
          groups={consultantsCalendar}
          items={items}
          defaultTimeStart={moment().add(-17, "day")}
          defaultTimeEnd={moment().add(4, "day")}
        />
      </div>
      {singleClient.listOfConsultants.map((consultant) => {
        const { name, id } = consultant;
        return <SingleConsultant name={name} id={id} key={id} />;
      })}
    </div>
  );
};

export default ConsultantsList;
