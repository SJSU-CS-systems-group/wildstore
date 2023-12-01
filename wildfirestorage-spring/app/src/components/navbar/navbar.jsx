import { GoGear, GoSignOut, GoMoon, GoSun } from 'react-icons/go';
import NavBarIcon from '../navbar-icon/navbarIcon';
import { useState, useEffect } from 'react';

const Navbar = () => {
    const [theme, setTheme] = useState('fantasy');
    const toggleTheme = () => {
        setTheme(theme === 'dark' ? 'fantasy' : 'dark');
    };
    // initially set the theme and "listen" for changes to apply them to the HTML tag
    useEffect(() => {
        document.querySelector('html').setAttribute('data-theme', theme);
    }, [theme]);

    const handleLogout = async () => {
        try {
            const response = await fetch('/logout', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
            });
            if (response.ok) {
                // Successful logout (Redirect to homepage -> goes to spring sign in)
                window.location.href="/"
            } else {
                // Logout failed
                console.error('Logout failed');
            }
        } catch (error) {
            console.error('Logout failed', error);
        }
    }

    return (
        <div id="navbar" className='flex flex-col align-end justify-center h-16 shadow-lg relative z-50'>
            <div className='flex flex-row justify-end mx-6'>
                <div className='flex items-center'>WILDSTORE</div>
                <div className='grow'></div>
                <div>
                    <NavBarIcon icon={<label className="swap swap-rotate">

                        {/* this hidden checkbox controls the state */}
                        <input type="checkbox" onClick={toggleTheme} />
                        <GoMoon className="swap-off" size={28} />
                        <GoSun className="swap-on" size={28} />
                    </label>} />
                </div>
                <NavBarIcon icon={<GoGear size="28" />} />
                <NavBarIcon icon={<GoSignOut size="28" onClick={handleLogout}/>}/>
                <div className='flex items-center'>
                    <div className="flex items-center avatar online placeholder">
                        <div className="bg-neutral-focus text-neutral-content rounded-full w-10">
                            <span className="text-xl">AM</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default Navbar;