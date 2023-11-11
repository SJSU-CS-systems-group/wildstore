import { GoHome, GoLocation, GoSearch, GoShareAndroid, GoPerson } from 'react-icons/go';
import SideBarIcon from '../sidebar-icon/sidebarIcon';
const SideBar = () => {
    return (
        <div>
            <SideBarIcon icon={<GoHome size="28"/>}/>
            <SideBarIcon icon={<GoLocation size="28"/>}/>
            <SideBarIcon icon={<GoSearch size="28"/>}/>
            <SideBarIcon icon={<GoShareAndroid size="28"/>}/>
            <SideBarIcon icon={<GoPerson size="28"/>}/>
        </div>
    );
};

export default SideBar;