import { useEffect, useState } from "react";
import { GoPerson, GoSearch, GoFileDirectory, GoKey } from "react-icons/go";
import { Link } from "react-router-dom";
import SideBarIcon from "../sidebar-icon/sidebarIcon";

const SideBar = () => {
  const [userRole, setUserRole] = useState("ROLE_GUEST");

  useEffect(() => {
    fetchUserRole();
  }, []);

  const fetchUserRole = async () => {
    const response = await fetch("/api/user/me", {
      method: "GET",
      credentials: "include",
    });
    if (!response.ok) {
      throw new Error("Failed to fetch user");
    }
    const data = await response.json();
    setUserRole(data.role);
  };

  return (
    <div>
      <Link to="/" aria-label="Open files">
        <SideBarIcon icon={<GoFileDirectory size="28" />} />
      </Link>
      {userRole === "ROLE_ADMIN" && (
        <Link to={"/users"}>
          <SideBarIcon icon={<GoPerson size="28" />} />
        </Link>
      )}
      <Link to="/search">
        <SideBarIcon icon={<GoSearch size="28" />} />
      </Link>
      <Link to="/token">
        <SideBarIcon icon={<GoKey size="28" />} />
      </Link>
    </div>
  );
};

export default SideBar;
