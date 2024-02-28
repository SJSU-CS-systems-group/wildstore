import './App.css';
import Home from './components/home/home';
import { BrowserRouter, Routes, Route } from "react-router-dom";
import Landing from './components/landing/landing';
import Token from './components/token/token';
import Forbidden from './components/forbidden/forbidden';

function App() {

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
