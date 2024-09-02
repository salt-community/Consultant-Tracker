import "./absence-info.css";
import SingleDetailField from "@/components/single-detail-field/single-detail-field";
import { TotalDaysStatisticsType } from "@/types";

type Props = {
  totalDaysStatistics: TotalDaysStatisticsType;
};

const AbsenceInfo = ({ totalDaysStatistics }: Props) => {
  const {
    totalVacationDaysUsed,
    totalSickDays,
    totalParentalLeaveDays,
    totalVABDays,
    totalUnpaidVacationDays,
  } = totalDaysStatistics;
  return (
    <div className="absence-info__wrapper">
      <h3>Total days used:</h3>
      <div className="absence-info__content">
        <SingleDetailField
          title="Vacation"
          content={`${totalVacationDaysUsed}`}
        />
        <SingleDetailField title="Sick" content={`${totalSickDays}`} />
        <SingleDetailField
          title="Parental leave"
          content={`${totalParentalLeaveDays}`}
        />
        <SingleDetailField title="VAB" content={`${totalVABDays}`} />
        <SingleDetailField
          title="Unpaid leave"
          content={`${totalUnpaidVacationDays}`}
        />
      </div>
    </div>
  );
};

export default AbsenceInfo;
