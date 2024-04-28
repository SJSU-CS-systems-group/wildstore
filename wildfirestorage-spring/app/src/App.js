import './App.css';
import Home from './components/home/home';
import { BrowserRouter, Routes, Route } from "react-router-dom";
import Landing from './components/landing/landing';
import Token from './components/token/token';
import Forbidden from './components/forbidden/forbidden';
import { useDispatch } from 'react-redux';
import { setOpaqueToken } from './redux/userSlice';
import { useEffect } from 'react';
import UsersList from './components/usersList/usersList';
import Layout from './components/layout/layout';
import ShareLinks from './components/sharelinks/shareLinks';

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
  }
  useEffect(() => {
    getToken();
  }, [])

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Landing />} />
        <Route path="/home" element={<Layout />}>
          <Route path="/home" element={<Home />}/>
          <Route path="/home/users" element={<UsersList />}/>
          <Route path="/home/share" element={<ShareLinks />}/>
        </Route>
        <Route path="/token" element={<Token />} />
        <Route path="/forbidden" element={<Forbidden />} />
        <Route path="/users" element={<UsersList />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App;
