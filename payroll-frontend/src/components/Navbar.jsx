import React from 'react';
import { Link } from 'react-router-dom';
import { useKeycloak } from '@react-keycloak/web';
import { useCart } from './CartContext.jsx';
import { AppBar, Box, Toolbar, Typography, Button, IconButton } from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import ConstructionIcon from '@mui/icons-material/Construction';
import "../index.css";



const Navbar = ({ onMenuClick }) => {

  const { keycloak, initialized } = useKeycloak();
  const { cartItems } = useCart();
  
  const getUsername = () => keycloak?.tokenParsed?.preferred_username || 'Usuario';

  return (
    
    <Box sx={{ flexGrow: 1 }}>
      <AppBar position="static" className="navbar" sx={{ backgroundColor: '#ffffff', color: '#BB0A21', fontFamily: 'monospace, Brush Script MT', borderRadius: '6px' , borderBlockEndColor: '#B497D6', borderBlockEndWidth: '5px', borderBlockEndStyle: 'solid' }}>
        <Toolbar>
          {keycloak.authenticated && (
            
            <IconButton
              size="large"
              edge="start"
              color="inherit"
              aria-label="menu"
              sx={{ mr: 2 }}
              onClick={onMenuClick} 
            >
              <MenuIcon />
            </IconButton>
          )}

          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }} className="navbar-title">
            <Link to="/home" className="navbar-link">
              <ConstructionIcon  sx={{marginTop: "10px"}}/>
              <span style={{marginLeft: '5px' }}>ToolRent</span>
            </Link>
          </Typography>

          {initialized && keycloak.authenticated && (
            <>
              <Link to="/loan-summary" className="navbar-link">
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <ShoppingCartIcon />
                  <span style={{ marginLeft: '5px' }}>({cartItems.length})</span>
                </Box>
              </Link>
              <Typography className="navbar-link" sx={{ mr: 2 }}>{getUsername()}</Typography>
              <Button sx={{ color: '#BB0A21', fontFamily: 'monospace, Brush Script MT' }} onClick={() => keycloak.logout()}>
                Logout
              </Button>
            </>
          )}
        </Toolbar>
      </AppBar>
    </Box>
  );
};

export default Navbar;