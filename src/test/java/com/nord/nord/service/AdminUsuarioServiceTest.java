package com.nord.nord.service;
import com.nord.nord.model.*;
import com.nord.nord.dto.*;
import com.nord.nord.repository.*;
import com.nord.nord.exception.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class AdminUsuarioServiceTest {
 UsuarioRepository users=mock(UsuarioRepository.class);
 PedidoRepository orders=mock(PedidoRepository.class);
 CarrinhoRepository carts=mock(CarrinhoRepository.class);
 AdminUsuarioService service=new AdminUsuarioService(users,orders,carts);
 Usuario admin=user(1L,Role.ADMIN), client=user(2L,Role.CLIENTE);
 Usuario user(Long id,Role role){var u=new Usuario();u.setId(id);u.setRole(role);u.setEmail("u"+id+"@example.com");return u;}
 void setup(){when(users.lockAll()).thenReturn(List.of(admin,client));}
 @Test void naoExcluiNemBloqueiaProprioAdmin(){setup();assertThrows(RegraDeNegocioException.class,()->service.deletar(1L,1L));assertThrows(RegraDeNegocioException.class,()->service.atualizar(1L,new AdminUsuarioRequest("A","a@example.com",Role.CLIENTE,true),1L));verify(users,never()).delete(any());}
 @Test void preservaUsuarioComPedidos(){setup();when(orders.existsByUsuario_Id(2L)).thenReturn(true);assertThrows(RegraDeNegocioException.class,()->service.deletar(2L,1L));verify(users,never()).delete(any());}
 @Test void desativarRevogaSessao(){setup();when(users.save(any())).thenAnswer(i->i.getArgument(0));var result=service.atualizar(2L,new AdminUsuarioRequest(" Cliente ","CLIENTE@example.com",Role.CLIENTE,false),1L);assertFalse(result.ativo());assertEquals("cliente@example.com",result.email());assertEquals(1,client.getAuthVersion());}
 @Test void excluiCarrinhoAntesDaConta(){setup();var cart=new Carrinho();when(carts.findByUsuario_Id(2L)).thenReturn(Optional.of(cart));service.deletar(2L,1L);var ordered=inOrder(carts,users);ordered.verify(carts).delete(cart);ordered.verify(users).delete(client);}
 @Test void adminRevogadoNaoExecutaOperacaoConcorrente(){setup();admin.setRole(Role.CLIENTE);assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.deletar(2L,1L));verify(users,never()).delete(any());}
}
