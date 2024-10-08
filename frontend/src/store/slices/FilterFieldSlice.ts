import { createSlice, PayloadAction } from "@reduxjs/toolkit";

interface FilterFieldState {
  filterClients: string[];
  filterName: string;
  debounceFilterName: string;
  filterPts: string[];
  listOfPts: string[];
  listOfClients: string[];
  includePgps: boolean;
}

const initialState: FilterFieldState = {
  filterClients: [],
  filterName: "",
  debounceFilterName: "",
  filterPts: [],
  listOfClients: [],
  listOfPts: [],
  includePgps: false,
};

const filterFieldSlice = createSlice({
  name: "filterField",
  initialState,
  reducers: {
    setFilterClients: (state, action: PayloadAction<string[]>) => {
      state.filterClients = action.payload;
    },
    setFilterPts: (state, action: PayloadAction<string[]>) => {
      state.filterPts = action.payload;
    },
    setFilterName: (state, action: PayloadAction<string>) => {
      state.filterName = action.payload;
    },
    setListOfPts: (state, action: PayloadAction<string[]>) => {
      state.listOfPts = action.payload;
    },
    setListOfClients: (state, action: PayloadAction<string[]>) => {
      state.listOfClients = action.payload;
    },
    setDebounceFilterName: (state, action: PayloadAction<string>) => {
      state.debounceFilterName = action.payload;
    },
    setIncludePgps: (state, action: PayloadAction<boolean>) => {
      state.includePgps = action.payload;
    },
  },
});

export const {
  setFilterClients,
  setDebounceFilterName,
  setListOfClients,
  setListOfPts,
  setFilterPts,
  setFilterName,
  setIncludePgps,
} = filterFieldSlice.actions;
export default filterFieldSlice.reducer;
