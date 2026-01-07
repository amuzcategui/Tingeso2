import * as React from "react";
import { useKeycloak } from "@react-keycloak/web";
import { useNavigate } from "react-router-dom";
import Box from "@mui/material/Box";
import Drawer from "@mui/material/Drawer";
import List from "@mui/material/List";
import Divider from "@mui/material/Divider";
import ListItemButton from "@mui/material/ListItemButton";
import ListItemIcon from "@mui/material/ListItemIcon";
import ListItemText from "@mui/material/ListItemText";

import HomeIcon from "@mui/icons-material/Home";
import InventoryIcon from "@mui/icons-material/Inventory";
import ShoppingCartIcon from "@mui/icons-material/ShoppingCart";
import HistoryIcon from "@mui/icons-material/History";
import AssessmentIcon from '@mui/icons-material/Assessment';
import AssignmentReturnIcon from '@mui/icons-material/AssignmentReturn';
import PeopleIcon from '@mui/icons-material/People';

export default function Menu({ open, toggleDrawer }) {
  const navigate = useNavigate();
  const { keycloak } = useKeycloak();

  const isAdmin = keycloak.authenticated && keycloak.tokenParsed?.realm_access?.roles.includes('ADMIN');


  const handleNavigation = (path) => {
    navigate(path);
    toggleDrawer(false)(); 
  };

  const listOptions = () => (
       <Drawer
        anchor="left"
        open={open}
        onClose={toggleDrawer(false)}
        PaperProps={{
            sx: {
            width: 250,
            bgcolor: '#B497D6', 
            color: '#111',       
            },
        }}>
        <Box
         
        role="presentation"
        onClick={toggleDrawer(false)} 
        onKeyDown={toggleDrawer(false)}

        >
        <List>
            
            <ListItemButton onClick={() => handleNavigation("/home")} sx={{ color: '#111' }}>
            <ListItemIcon>
                <HomeIcon sx={{ color: '#52154E' }}/>
            </ListItemIcon>
            <ListItemText  sx={{ color: '#111' }} primary="Catálogo" />
            </ListItemButton >

            <ListItemButton onClick={() => handleNavigation("/loan-summary")}>
            <ListItemIcon>
                <ShoppingCartIcon sx={{ color: '#52154E' }}/>
            </ListItemIcon>
            <ListItemText sx={{ color: '#111' }} primary="Mi Carrito" />
            </ListItemButton>

            <ListItemButton onClick={() => handleNavigation("/my-loans")}>
            <ListItemIcon>
                <HistoryIcon sx={{ color: '#52154E' }}/>
            </ListItemIcon>
            <ListItemText sx={{ color: '#111' }} primary="Mis Préstamos" />
            </ListItemButton>
        </List>

        
        {isAdmin && (
            <>
            <Divider />
            <List>
                <ListItemButton onClick={() => handleNavigation("/inventory")}>
                <ListItemIcon>
                    <InventoryIcon sx={{ color: '#52154E' }}/>
                </ListItemIcon>
                <ListItemText sx={{ color: '#111' }} primary="Gestionar Inventario" />
                </ListItemButton>
                

                <ListItemButton onClick={() => handleNavigation("/kardex")}>
                  <ListItemIcon>
                    <AssessmentIcon sx={{ color: '#52154E' }}/>
                  </ListItemIcon>
                  <ListItemText sx={{ color: '#111' }} primary="Reportes (Kardex)" />
                </ListItemButton>

                <ListItemButton onClick={() => handleNavigation("/return-loan")}>
                  <ListItemIcon>
                    <AssignmentReturnIcon sx={{ color: '#52154E' }}/>
                    </ListItemIcon>
                  <ListItemText sx={{ color: '#111' }} primary="Devolver Préstamo" />
                </ListItemButton>

                <ListItemButton onClick={() => handleNavigation("/customers")}>
                  <ListItemIcon><PeopleIcon sx={{ color: '#52154E' }} /></ListItemIcon>
                  <ListItemText sx={{ color: '#111' }} primary="Gestionar Clientes" />
                </ListItemButton>


            </List>
            </>
        )}
        </Box>
    </Drawer>
  );

  return (
    <div >
      <Drawer anchor={"left"} open={open} onClose={toggleDrawer(false)}>
        {listOptions()}
      </Drawer>
    </div>
  );
}