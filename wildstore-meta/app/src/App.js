import { useEffect } from 'react';
import { useDispatch } from 'react-redux';
import { BrowserRouter, Route, Routes } from "react-router-dom";
import './App.css';
import FileBrowser from './components/file-browser/FileBrowser';
import Forbidden from './components/forbidden/forbidden';
import Home from './components/home/home';
import Landing from './components/landing/landing';
import Layout from './components/layout/layout';
import ShareLinks from './components/sharelinks/shareLinks';
import Token from './components/token/token';
import UsersList from './components/usersList/usersList';
import { setOpaqueToken } from './redux/userSlice';

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
      window.location.href = response.url;
    }
    let d = await response.text();
    dispatch(setOpaqueToken(d));
  }
  // eslint-disable-next-line react-hooks/exhaustive-deps
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
        <Route path="/files" element={<Layout />}>
          <Route path="/files" element={<FileBrowser mode="download" />}/>
          <Route path="/files/share" element={<FileBrowser mode="share" />}/>
        </Route>
        <Route path="/filescontents" element={<Layout />}>
          <Route path="/filescontents" element={<FileBrowser mode="download" />}/>
        </Route>
        <Route path="/file-contents" element={<Layout />}>
          <Route path="/file-contents" element={<FileBrowser mode="download" />}/>
        </Route>
        <Route path="/token" element={<Token />} />
        <Route path="/forbidden" element={<Forbidden />} />
        <Route path="/users" element={<UsersList />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App;
