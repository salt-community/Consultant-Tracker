"use client"
import Client from "@/components/consultant-detail/client/client";
import PersonalData from "@/components/consultant-detail/personal-data/personal-data";
import Schedule from "@/components/consultant-detail/schedule/schedule";
import "./page.css"
import React, {SyntheticEvent, useState} from "react";
import VacationInfo from "@/components/consultant-detail/vacation-info/vacation-info";
import TabsComponent from "@/components/tabs/tabs";
import Breadcrumbs from "@/components/breadcrumbs/breadcrumbs";
import {useDetailsContext} from "@/context/details";
import BasicInfo from "@/components/consultant-detail/basic-info/basic-info";

const ConsultantDetail = () => {
  const [value, setValue] = useState('personal');

  const handleChange = (event: SyntheticEvent, newValue: string) => {
    setValue(newValue);
  };
  const details = useDetailsContext();
  const content = () => {
    switch (value) {
      case "schedule":
        return <Schedule/>
      case "vacation":
        return <VacationInfo/>
      case "clients":
        return <Client/>
      default:
        return <PersonalData/>
    }
  }
  return (
    <>
      <Breadcrumbs current={`${details.name}`}/>
      <BasicInfo />
      <TabsComponent value={value} handleChange={handleChange}/>
      <div className="consultant-detail__card">{content()}</div>
    </>
  );
};

export default ConsultantDetail;
