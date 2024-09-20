import {createSlice, PayloadAction} from "@reduxjs/toolkit";

interface TokenState {
  token: string,
}

const initialState: TokenState = {
  token: ""
}

const tokenSlice = createSlice({
  name: 'pagination',
  initialState,
  reducers: {
    setToken: (state, action: PayloadAction<string>) => {
      state.token = action.payload
    }
  }
})

export const {
  setToken
} = tokenSlice.actions;
export default tokenSlice.reducer;