import './App.css';
import Home from './components/home/home';
import { BrowserRouter, Routes, Route } from "react-router-dom";
import Landing from './components/landing/landing';
import Token from './components/token/token';
import Forbidden from './components/forbidden/forbidden';
import { useDispatch } from 'react-redux';
import { setOpaqueToken } from './redux/userSlice';
import { useEffect } from 'react';

function App() {
  const dispatch = useDispatch();
  
  const getToken = async () => {
    const response = await fetch("/api/oauth/token", {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        "Accept": "text/plain, application/json",
      },
      credentials: "include",
      redirect: "follow",
    });
    if (response.redirected) {
      document.location = response.url;
    }
    let d = await response.text();
    dispatch(setOpaqueToken(d));
    console.log("tokennnn", d)
  }
  useEffect(() => {
    getToken();
  }, [])

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Landing />} />
        <Route path="/home" element={<Home />} />
        <Route path="/token" element={<Token />} />
        <Route path="/forbidden" element={<Forbidden />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App;
