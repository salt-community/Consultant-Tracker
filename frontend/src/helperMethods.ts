import dayjs from "dayjs";
import {ConsultantItemsType} from "./types";

export const getDatesForRemainingTime = (
  id: number,
  items: ConsultantItemsType[]
) => {
  const matchingDates = items
    .filter((i) => i.group === id)
    .map((d) => d.start_time);
  const workedTime = matchingDates.length * 8;
  const remainingHours = 88 - workedTime;
  const remainingDays = remainingHours / 8;
  const lastWorkedDay = dayjs(matchingDates[matchingDates.length - 1]);
  const timezoneOffset = lastWorkedDay.utcOffset();
  const startDate = dayjs()
    .set("day", lastWorkedDay.day() + 1)
    .set("month", lastWorkedDay.month())
    .set("year", lastWorkedDay.year())
    .set("hours", 0)
    .set("minutes", 0)
    .set("seconds", 0);
  console.log('first startDate', startDate);

  const tempEndDate = dayjs(startDate)
    .set("date", startDate.date() + remainingDays - 1)
    .set("hours", 23)
    .set("minutes", 59)
    .set("seconds", 59);

  const endDate = accountForNonWorkingDays(startDate, tempEndDate);

  return JSON.stringify({
    start_date: startDate,
    end_date: endDate,
    remainingHours: remainingHours,
  });
};

function accountForNonWorkingDays(
  startDate: dayjs.Dayjs,
  tempEndDate: dayjs.Dayjs
) {
  let nonWorkDaysCount = 0;

  const daysDiff = tempEndDate.diff(startDate, "day");

  for (let i = 0; i <= daysDiff + 1; i++) {
    const date = startDate.add(i, "day")
    const day = date.day();
    const saturday = 6;
    const sunday = 0;
    if (day === saturday || day === sunday) {
      nonWorkDaysCount++;
    }
  }
  return dayjs(tempEndDate).add(nonWorkDaysCount, "day");
}
